package com.orbitalhq.query.runtime.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.AuthClaimType
import com.orbitalhq.FactSetId
import com.orbitalhq.FactSets
import com.orbitalhq.logging.MDCContextKeys.ClientQueryId
import com.orbitalhq.logging.MDCContextKeys.QueryId
import com.orbitalhq.VyneProvider
import com.orbitalhq.auth.EmptyAuthenticationToken
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.auth.authentication.toVyneUser
import com.orbitalhq.auth.getAuthClaimsAsFacts
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.logging.MDCContextKeys.QueryName
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.EmitMetrics
import com.orbitalhq.query.Fact
import com.orbitalhq.query.HistoryEventConsumerProvider
import com.orbitalhq.query.Query
import com.orbitalhq.query.QueryCancelledException
import com.orbitalhq.query.QueryErrorEvent
import com.orbitalhq.query.QueryFailedException
import com.orbitalhq.query.QueryMode
import com.orbitalhq.query.QueryResponse
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.QueryStartEvent
import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.SearchFailedException
import com.orbitalhq.query.runtime.FailedSearchResponse
import com.orbitalhq.query.runtime.QueryServiceApi
import com.orbitalhq.query.runtime.core.monitor.ActiveQueryMonitor
import com.orbitalhq.query.tracing.TraceContext
import com.orbitalhq.query.tracing.TracingEvent
import com.orbitalhq.query.tracing.TracingEventSink
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.websocket.WebSocketController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import mu.KotlinLogging
import org.reactivestreams.Publisher
import org.slf4j.MDC
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.PooledDataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.CorePublisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.security.Principal
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException

const val TEXT_CSV = "text/csv"
const val TEXT_CSV_UTF_8 = "$TEXT_CSV;charset=UTF-8"


/**
 * We have to do some funky serialization for QueryResult,
 * so controller methods are marked to return the Json directly, rather
 * than allow the default Jackson serialization to take hold
 */
typealias QueryResponseString = String

private val logger = KotlinLogging.logger {}

/**
 * Main entry point for submitting queries to Vyne.
 */
@FlowPreview
@RestController
class QueryService(
   private val schemaProvider: SchemaProvider,
   val vyneProvider: VyneProvider,
   val historyWriterProvider: HistoryEventConsumerProvider,
   val objectMapper: ObjectMapper,
   val activeQueryMonitor: ActiveQueryMonitor,
   private val queryResponseFormatter: QueryResponseFormatter,
   private val traceEventSink: TracingEventSink
) : QueryServiceApi, WebSocketController {


   private fun queryResultToResponseEntity(
      queryResult: QueryResponse,
      resultMode: ResultMode,
      contentType: String,
      queryOptions: QueryOptions
   ): ResponseEntity<Publisher<Any>> {
      val httpStatus = when (queryResult) {
         is QueryResult -> HttpStatus.OK
         is FailedSearchResponse -> HttpStatus.BAD_REQUEST
         else -> error("Unknown type of QueryResponse received:  ${queryResult::class.simpleName}")
      }
      val (contentType, body) = convertToExpectedResult(queryResult, resultMode, contentType, queryOptions)
      return ResponseEntity.status(httpStatus)
         .header("x-vyne-query-id", queryResult.queryId)
         .header("x-vyne-client-query-id", queryResult.clientQueryId)
         .header(HttpHeaders.CONTENT_TYPE, contentType)
         .headers { httpHeaders ->
            queryResult.responseHeaders?.forEach { (t, u) ->
               u.forEach { headerValue ->
                  httpHeaders.add(t, headerValue)
               }
            }
         }
         .body(body)
   }


   private fun convertToExpectedResult(
      queryResult: QueryResponse,
      resultMode: ResultMode,
      requestedContentType: String,
      queryOptions: QueryOptions
   ): Pair<String, CorePublisher<Any>> {
      val (contentType, flow) = queryResponseFormatter.convertToSerializedContent(
         queryResult,
         resultMode,
         requestedContentType,
         queryOptions
      )
      if (queryResult is FailedSearchResponse) {
         // I don't think this Mono actually gets used, but we need to satisfy the API contract
         return contentType to Mono.error(QueryFailedException(queryResult.message))
      }
      if (queryResult.responseType == null) {
         TODO("How to handle this when responseType == null?")
      }
      return if (queryResult.responseType!!.isCollection) {
         contentType to flow.asFlux()
      } else {
         contentType to flow.asFlux().single()
      }
   }

   suspend fun <T> monitored(
      query: TaxiQlQuery,
      clientQueryId: String?,
      queryId: String, vyneUser: VyneUser?,
      block: suspend () -> T
   ): T {

      activeQueryMonitor.reportStart(queryId, clientQueryId, query)
      return block.invoke()
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   @PostMapping(
      value = ["/api/vyneql", "/api/taxiql"],
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "application/taxiql"],
      produces = [TEXT_CSV]
   )
   @Deprecated("Use model formats, which support CSV")
   suspend fun submitVyneQlQueryCSV(
      @RequestBody query: TaxiQLQueryString,
      @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String,
      @RequestParam("clientQueryId", required = false) clientQueryId: String? = null,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode = ResultMode.RAW,
      auth: Authentication? = null,
   ): ResponseEntity<Flow<String>> {

      val user = auth?.toVyneUser()
      val (response, queryOptions) = vyneQLQuery(
         query,
         user,
         clientQueryId = clientQueryId,
         queryId = UUID.randomUUID().toString()
      )

      return queryResultToResponseEntity(
         response,
         resultMode,
         contentType,
         queryOptions
      ) as ResponseEntity<Flow<String>>

   }

   /**
    * The main query entry point when sending queries outside of the UI.
    *
    * Intentionally not a suspend function.
    * We need to return a CorePublisher here, rather than Flow, to allow
    * us to serialize either:
    *  - Array responses for fluxes (where we return a list)
    *  - Mono responses (as objects) for single item queries
    *
    *  Because of Spring Security limitations, must be a Mono<ResponseEntity<Publisher<>>>
    *
    *  This is because:
    *    - Mono<   .... All Spring Security reactive functions must return either a reactive type or a Kotlin Flow
    *    -      ResponseEntity<  .... We want to control the headers
    *    -                     Publisher<>   ..... Could be either a Mono or a Flu             x
    */
   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   @PostMapping(
      value = ["/api/vyneql", "/api/taxiql"],
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "application/taxiql"],
      produces = [MediaType.APPLICATION_JSON_VALUE]
   )
   override fun submitVyneQlQuery(
      @RequestBody query: TaxiQLQueryString,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestHeader(
         value = "Accept",
         defaultValue = MediaType.APPLICATION_JSON_VALUE
      ) contentType: String,
      auth: Authentication?,
      @RequestParam("clientQueryId", required = false) clientQueryId: String?,
   ): Mono<ResponseEntity<Publisher<Any>>> {
      return Mono.fromCallable {
         val user = auth?.toVyneUser()

         // Note: This isn't actually a blocking query (we just
         // messed up by using suspend in the wrong place.
         // The query doesn't actually start until we subscribe on the flow in the QueryResponse returned.
         val (response, queryOptions) = runBlocking {
            vyneQLQuery(
               query,
               user,
               clientQueryId = clientQueryId,
               queryId = clientQueryId ?: UUID.randomUUID().toString()
            )
         }
         queryResultToResponseEntity(response, resultMode, contentType, queryOptions)
      }


   }

   /**
    * Overload which allows taking a set of arguments.
    * This is used when calling saved queries with @Http endpoints exposed.
    * Invoked by the LocalQueryDispatcher, when a remote dispatcher (eg.,
    * calling a Cloud Function) has not been provided.
    *
    * Intentionally not a suspend function.
    * We need to return a CorePublisher here, rather than Flow, to allow
    * us to serialize either:
    *  - Array responses for fluxes (where we return a list)
    *  - Mono responses (as objects) for single item queries
    */
   fun submitVyneQlQuery(
      query: TaxiQLQueryString,
      resultMode: ResultMode,
      contentType: String,
      auth: Authentication?,
      clientQueryId: String?,
      arguments: Map<String, Any?>
   ): Pair<ResponseEntity<Publisher<Any>>, Flux<QueryErrorEvent>> {

      val user = auth?.toVyneUser()

      // Note: This isn't actually a blocking query (we just
      // messed up by using suspend in the wrong place.
      // The query doesn't actually start until we subscribe on the flow in the QueryResponse returned.
      val (response, queryOptions) = runBlocking {
         vyneQLQuery(
            query,
            user,
            clientQueryId = clientQueryId,
            queryId = clientQueryId ?: UUID.randomUUID().toString(),
            arguments = arguments
         )
      }
      val errors = when (response) {
         is QueryResult -> response.errors
         else -> Flux.empty()
      }
      return queryResultToResponseEntity(response, resultMode, contentType, queryOptions) to errors
   }

   /**
    * Endpoint for submitting a TaxiQL query, and receiving an event stream back.
    * Note that POST is used here for backwards compatability with existing approaches,
    * however browsers are unable to issue requests using SSE on anything other than a GET.
    *
    * Also, this endpoint is exposed under both /vyneql (legacy) and /taxiql (renamed).
    */
   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   @PostMapping(
      value = ["/api/vyneql", "/api/taxiql"],
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "application/taxiql"],
      produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
   )
   override suspend fun submitVyneQlQueryStreamingResponse(
      @RequestBody query: TaxiQLQueryString,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestHeader(
         value = "ContentSerializationFormat",
         defaultValue = MediaType.APPLICATION_JSON_VALUE
      ) contentType: String,
      auth: Authentication?,
      @RequestParam("clientQueryId", required = false) clientQueryId: String?
   ): Flow<Any?> {
      return getVyneQlQueryStreamingResponse(query, resultMode, contentType, auth, clientQueryId)
   }


   /**
    * Endpoint for submitting a TaxiQL query, and receiving an event stream back.
    * Browsers cannot submit POST requests for SSE responses (only GET), hence having the query in the queryString.
    * Accept header is used for SSE (text/event-stream) so a custom header name (ContentSerializationFormat) is needed
    * for the actual content type.
    *
    * Also, this endpoint is exposed under both /vyneql (legacy) and /taxiql (renamed).
    */
   @GetMapping(value = ["/api/vyneql", "/api/taxiql"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   suspend fun getVyneQlQueryStreamingResponse(
      @RequestParam("query") query: TaxiQLQueryString,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestHeader(
         value = "ContentSerializationFormat",
         defaultValue = MediaType.APPLICATION_JSON_VALUE
      ) contentType: String,
      auth: Authentication? = null,
      @RequestParam("clientQueryId", required = false) clientQueryId: String? = null
   ): Flow<Any?> {
      if (resultMode == ResultMode.VERBOSE && !listOf(
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_CBOR_VALUE
         ).contains(contentType)
      ) {
         throw IllegalArgumentException("Only JSON (application/json) & CBOR (application/cbor) are supported for streaming responses with verbose mode. ")
      }
      val user = auth?.toVyneUser()
      val queryId = UUID.randomUUID().toString()
      MDC.put(QueryId, queryId)
      val (queryResponse, queryOptions) = vyneQLQuery(query, user, clientQueryId, queryId)
      return when (queryResponse) {
         is FailedSearchResponse -> flowOf(queryResponse)
         is QueryResult -> {
            val resultSerializer =
               this.queryResponseFormatter.buildStreamingSerializer(
                  resultMode,
                  queryResponse,
                  contentType,
                  queryOptions
               )
            queryResponse.results
               .catch { throwable ->
                  when (throwable) {
                     is SearchFailedException -> {
                        emit(ErrorType.errorMessage(throwable.message ?: "No message provided", schemaProvider.schema))
                        logger.warn { "Query $queryId failed with a SearchFailedException. ${throwable.message!!}" }
                     }

                     is QueryCancelledException -> {
                        emit(ErrorType.errorMessage(throwable.message ?: "No message provided", schemaProvider.schema))
                        //emit(QueryCancelledType.cancelled(throwable.message ?: "No message provided"))
                        logger.info { "Query $queryId was cancelled" }
                     }

                     else -> {
                        emit(ErrorType.errorMessage(throwable.message ?: "No message provided", schemaProvider.schema))
                        logger.error(throwable) { "Query $queryId failed with an unexpected exception of type: ${throwable::class.simpleName}.  ${throwable.message ?: "No message provided"}" }
                     }
                  }
               }
               .map {
                  resultSerializer.serialize(it, queryResponse.schema)
               }
         }

         else -> error("Unhandled type of QueryResponse for query $queryId - received ${queryResponse::class.simpleName}")
      }
   }

   private val vyneQlDispatcher =
      Executors.newFixedThreadPool(10).asCoroutineDispatcher()


   override val paths: List<String> = listOf("/api/query/taxiql")

   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   override fun handle(session: WebSocketSession): Mono<Void> {
      val sink = Sinks.many().replay().latest<String>()
      val output = sink.asFlux()
         .map { entry -> session.textMessage(entry.toString()) }

      // Create a coroutine scope that can be cancelled
      val queryScope = CoroutineScope(vyneQlDispatcher)
      var clientQueryId: String? = null

      session.receive()
         .doOnComplete {
            logger.info { "Websocket query with client queryId $clientQueryId was closed gracefully by the consumer - cancelling the query" }
            queryScope.cancel()
         }
         .doOnCancel {
            logger.info { "Websocket query with client queryId $clientQueryId was closed ungracefully by the consumer - cancelling the query" }
            queryScope.cancel()
         }
         .flatMap { message ->
            session.handshakeInfo.principal.defaultIfEmpty(EmptyAuthenticationToken)
               .map { auth: Principal -> auth to message }
         }
         .subscribe { (auth, message) ->
            val websocketQuery = objectMapper.readValue<WebsocketQuery>(message.payloadAsText)
            clientQueryId = websocketQuery.clientQueryId
            MDC.put(ClientQueryId, clientQueryId)
            queryScope.launch(MDCContext(MDC.getCopyOfContextMap())) {
               try {
                  val nullableAuth = EmptyAuthenticationToken.nullIfEmpty(auth) as Authentication?
                  getVyneQlQueryStreamingResponse(
                     websocketQuery.query,
                     websocketQuery.resultMode,
                     MediaType.APPLICATION_JSON_VALUE,
                     clientQueryId = websocketQuery.clientQueryId,
                     auth = nullableAuth
                  )
                     .cancellable()
                     .onCompletion { error ->
                        if (error == null) {
                           sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST)
                        } else {
                           sink.emitError(error, Sinks.EmitFailureHandler.FAIL_FAST)
                        }
                        session.close(CloseStatus.NORMAL)
                           .subscribe()
                     }
                     .collect { emittedResult ->
                        ensureActive()
                        val json = objectMapper.writeValueAsString(emittedResult)
                        sink.emitNext(json, Sinks.EmitFailureHandler.FAIL_FAST)
                     }

               } catch (e: Exception) {
                  when (e) {
                     is CancellationException -> {
                        logger.info { "Query cancelled due to websocket disconnection" }
                        sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST)
                     }

                     else -> {
                        // Compilation exceptions hit here, before the flow exists.
                        val errorMessage = FailedSearchResponse(
                           e.message ?: e::class.simpleName ?: "An unknown error occurred",
                           null,
                           websocketQuery.clientQueryId
                        )
                        val json = objectMapper.writeValueAsString(errorMessage)
                        sink.emitNext(json, Sinks.EmitFailureHandler.FAIL_FAST)

                        sink.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST)
                        session.close(CloseStatus.BAD_DATA)
                           .subscribe()
                     }
                  }
               }
            }
         }


      return session.send(output).doOnDiscard(PooledDataBuffer::class.java, DataBufferUtils::release)
   }

   private fun extractJwtClaimFactFromQueryParameters(
      schema: Schema,
      parameters: List<lang.taxi.query.Parameter>
   ): String? {
      val jwtParameter =
         parameters.firstOrNull { it.type.inheritsFrom(schema.taxiType(AuthClaimType.AuthClaimsTypeName)) }
      return jwtParameter?.type?.qualifiedName

   }

   private suspend fun vyneQLQuery(
      query: TaxiQLQueryString,
      vyneUser: VyneUser? = null,
      clientQueryId: String?,
      queryId: String,
      traceId: String = TracingEvent.newTraceId(),
      arguments: Map<String, Any?> = emptyMap()
   ): Pair<QueryResponse, QueryOptions> {
      logger.info { "[$queryId] $query" }
      val schema = schemaProvider.schema
      val (taxiQlQuery, queryOptions, querySchema) = schema.parseQuery(query)
      MDC.put(QueryName, taxiQlQuery.name.parameterizedName)
      return monitored(query = taxiQlQuery, clientQueryId = clientQueryId, queryId = queryId, vyneUser = vyneUser) {
         logger.info { "using cache ${queryOptions.cachingStrategy}" }

         // TODO : MP - 2-Aug-24
         // Need to talk to Serhat about this.
         // Here, we're extracting facts presented in the parameters that extend from the AuthClaims
         // type. This was to support scenarios where we explcitly want to promote a provided
         // header (eg a Zoho auth token), to provide downstream to requests.
         // However, we also need to pull out the user facts for policies
         // I think using this approach, we end up with duplicates.
         // Need to work out a better way.
         val userAuthTokenFacts = vyneUser.getAuthClaimsAsFacts(schema)
         val executionContextFacts = vyneUser.facts(
            extractJwtClaimFactFromQueryParameters(schema, taxiQlQuery.parameters)
         ) + userAuthTokenFacts

         val vyne = vyneProvider.createVyne(executionContextFacts + userAuthTokenFacts, querySchema, queryOptions)
         val historyWriterEventConsumer = historyWriterProvider.createEventConsumer(queryId, vyne.schema)
         val response = try {
            val eventDispatcherForQuery =
               activeQueryMonitor.eventDispatcherForQuery(queryId, TraceContext.forTraceId(traceId, queryId, traceEventSink), listOf(historyWriterEventConsumer))
            vyne.query(
               taxiQlQuery,
               queryId = queryId,
               clientQueryId = clientQueryId,
               eventBroker = eventDispatcherForQuery,
               arguments = arguments,
               queryOptions = queryOptions,
               querySchema = querySchema,
               executionContextFacts = executionContextFacts,

               )
         } catch (e: lang.taxi.CompilationException) {
            logger.info("The query failed compilation: ${e.message}")
            /**
             * Query failed due to compilation even without start execution.
             * We need to emit the QueryStart event manually so that analytics records are persisted for query compilation as well.
             */
            historyWriterEventConsumer.handleEvent(
               QueryStartEvent(
                  queryId = queryId,
                  timestamp = Instant.now(),
                  taxiQuery = query,
                  query = null,
                  clientQueryId = clientQueryId ?: "",
                  message = "",
                  anonymousTypes = emptySet()
               )
            )
            val failedSearchResponse = FailedSearchResponse(
               message = e.message!!, // Message contains the error messages from the compiler
               profilerOperation = null,
               clientQueryId = clientQueryId,
               queryId = queryId
            )
            failedSearchResponse
         } catch (e: SearchFailedException) {
            FailedSearchResponse(e.message!!, e.profilerOperation, queryId = queryId)

         } catch (e: NotImplementedError) {
            // happens when Schema is empty
            FailedSearchResponse(e.message!!, null, queryId = queryId)
         } catch (e: QueryCancelledException) {
            FailedSearchResponse(e.message!!, null, queryId = queryId)
         } catch (e: Exception) {
            FailedSearchResponse(e.message!!, null, queryId = queryId)
         }
         QueryLifecycleEventObserver(historyWriterEventConsumer, activeQueryMonitor)
            .responseWithQueryHistoryListener(query, response) to queryOptions
      }
   }


   private fun parseFacts(facts: List<Fact>, schema: Schema): List<Pair<TypedInstance, FactSetId>> {

      return facts.map { (typeName, value, factSetId) ->
         TypedInstance.from(schema.type(typeName), value, schema, source = Provided) to factSetId
      }
   }
}

data class WebsocketQuery(
   val clientQueryId: String,
   val query: String,
   // Default for the UI. TODO : Make the default RAW
   val resultMode: ResultMode = ResultMode.TYPED
)

