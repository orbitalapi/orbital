package com.orbitalhq.query.runtime.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.FactSetId
import com.orbitalhq.FactSets
import com.orbitalhq.Vyne
import com.orbitalhq.VyneProvider
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.auth.authentication.toVyneUser
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.*
import com.orbitalhq.query.runtime.FailedSearchResponse
import com.orbitalhq.query.runtime.QueryServiceApi
import com.orbitalhq.query.runtime.core.monitor.ActiveQueryMonitor
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.websocket.WebSocketController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import lang.taxi.query.TaxiQLQueryString
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors

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
   val metricsEventConsumer: MetricsEventConsumer,
   private val queryResponseFormatter: QueryResponseFormatter,
) : QueryServiceApi, WebSocketController {


   @Deprecated("Use taxiQL endpoints instead")
   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   @PostMapping(
      "/api/query",
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "application/taxiql"],
      produces = [MediaType.APPLICATION_JSON_VALUE]
   )
   suspend fun submitQuery(
      @RequestBody query: Query,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String,
      @RequestParam("clientQueryId", required = false) clientQueryId: String? = null
   ): ResponseEntity<Flow<Any>> {

      val queryResult = executeQuery(query, clientQueryId)
      return queryResultToResponseEntity(queryResult, resultMode, contentType, QueryOptions())
   }

   private fun queryResultToResponseEntity(
      queryResult: QueryResponse,
      resultMode: ResultMode,
      contentType: String,
      queryOptions: QueryOptions
   ): ResponseEntity<Flow<Any>> {
      val httpStatus = when (queryResult) {
         is QueryResult -> HttpStatus.OK
         is FailedSearchResponse -> HttpStatus.BAD_REQUEST
         else -> error("Unknown type of QueryResponse received:  ${queryResult::class.simpleName}")
      }
      return ResponseEntity.status(httpStatus)
         .header("x-vyne-query-id", queryResult.queryId)
         .header("x-vyne-client-query-id", queryResult.clientQueryId)
         .body(convertToExpectedResult(queryResult, resultMode, contentType, queryOptions))
   }


   private fun convertToExpectedResult(
      queryResult: QueryResponse,
      resultMode: ResultMode,
      contentType: String,
      queryOptions: QueryOptions
   ): Flow<Any> {
      return queryResponseFormatter.convertToSerializedContent(queryResult, resultMode, contentType, queryOptions)
   }

   suspend fun <T> monitored(
      query: TaxiQLQueryString,
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

   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   @PostMapping(
      value = ["/api/vyneql", "/api/taxiql"],
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "application/taxiql"],
      produces = [MediaType.APPLICATION_JSON_VALUE]
   )
   override suspend fun submitVyneQlQuery(
      @RequestBody query: TaxiQLQueryString,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestHeader(
         value = "Accept",
         defaultValue = MediaType.APPLICATION_JSON_VALUE
      ) contentType: String,
      auth: Authentication?,
      @RequestParam("clientQueryId", required = false) clientQueryId: String?
   ): ResponseEntity<Flow<Any>> {

      val user = auth?.toVyneUser()
      val (response, queryOptions) = vyneQLQuery(
         query,
         user,
         clientQueryId = clientQueryId,
         queryId = clientQueryId ?: UUID.randomUUID().toString()
      )
      return queryResultToResponseEntity(response, resultMode, contentType, queryOptions)
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
                        emit(ErrorType.error(throwable.message ?: "No message provided", schemaProvider.schema))
                        logger.warn { "Query $queryId failed with a SearchFailedException. ${throwable.message!!}" }
                     }

                     is QueryCancelledException -> {
                        emit(ErrorType.error(throwable.message ?: "No message provided", schemaProvider.schema))
                        //emit(QueryCancelledType.cancelled(throwable.message ?: "No message provided"))
                        logger.info { "Query $queryId was cancelled" }
                     }

                     else -> {
                        emit(ErrorType.error(throwable.message ?: "No message provided", schemaProvider.schema))
                        logger.error { "Query $queryId failed with an unexpected exception of type: ${throwable::class.simpleName}.  ${throwable.message ?: "No message provided"}" }
                     }
                  }
               }
               .map {
                  resultSerializer.serialize(it)
               }
         }

         else -> error("Unhandled type of QueryResponse for query $queryId - received ${queryResponse::class.simpleName}")
      }
   }

   private val vyneQlDispatcher =
      Executors.newFixedThreadPool(10).asCoroutineDispatcher()


   override val paths: List<String> = listOf("/api/query/taxiql")
   override fun handle(session: WebSocketSession): Mono<Void> {
      val sink = Sinks.many().replay().latest<String>()
      val output = sink.asFlux()
         .map { entry -> session.textMessage(entry.toString()) }

      session.receive()
         .subscribe { message ->
            val websocketQuery = objectMapper.readValue<WebsocketQuery>(message.payloadAsText)

            CoroutineScope(vyneQlDispatcher).launch {
               try {
                  getVyneQlQueryStreamingResponse(
                     websocketQuery.query,
                     websocketQuery.resultMode,
                     MediaType.APPLICATION_JSON_VALUE,
                     clientQueryId = websocketQuery.clientQueryId
                  ).onCompletion { error ->
                     if (error == null) {
                        sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST)
                     } else {
                        sink.emitError(error, Sinks.EmitFailureHandler.FAIL_FAST)
                     }
                  }
                     .collect { emittedResult ->
                        val json = objectMapper.writeValueAsString(emittedResult)
                        sink.emitNext(json, Sinks.EmitFailureHandler.FAIL_FAST)
                     }
                  session.close(CloseStatus.NORMAL)
               } catch (e: Exception) {
                  // Compilation exceptions hit here, before the flow exists.
                  val errorMessage = FailedSearchResponse(
                     e.message!!,
                     null,
                     websocketQuery.clientQueryId
                  )
                  val json = objectMapper.writeValueAsString(errorMessage)
                  sink.emitNext(json, Sinks.EmitFailureHandler.FAIL_FAST)

                  sink.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST)
                  session.close(CloseStatus.BAD_DATA)
               }

            }
         }


      return session.send(output)
   }

   suspend fun doVyneMonitoredWork(
      vyneUser: VyneUser? = null,
      schema: Schema,
      queryCallback: suspend (Vyne, QueryContextEventBroker) -> QueryResponse
   ): QueryResponse {
      val queryId: String = UUID.randomUUID().toString()
      val vyne = vyneProvider.createVyne(
         vyneUser.facts(),
         schema
      )

      val historyWriterEventConsumer = historyWriterProvider.createEventConsumer(queryId, schema)
      val eventDispatcherForQuery =
         activeQueryMonitor.eventDispatcherForQuery(queryId, listOf(historyWriterEventConsumer))

      val queryResponse = queryCallback(vyne, eventDispatcherForQuery)
      return QueryLifecycleEventObserver(historyWriterEventConsumer, activeQueryMonitor)
         .responseWithQueryHistoryListener("Adhoc query", queryResponse)
   }

   private suspend fun vyneQLQuery(
      query: TaxiQLQueryString,
      vyneUser: VyneUser? = null,
      clientQueryId: String?,
      queryId: String
   ): Pair<QueryResponse, QueryOptions> =
      monitored(query = query, clientQueryId = clientQueryId, queryId = queryId, vyneUser = vyneUser) {
         logger.info { "[$queryId] $query" }
         val schema = schemaProvider.schema
         val (taxiQlQuery, queryOptions) = schema.parseQuery(query)
         logger.info { "[$queryId] using cache ${queryOptions.cachingStrategy}" }
         val vyne = vyneProvider.createVyne(vyneUser.facts(), schema, queryOptions)
         val historyWriterEventConsumer = historyWriterProvider.createEventConsumer(queryId, vyne.schema)
         val response = try {
            val eventDispatcherForQuery =
               activeQueryMonitor.eventDispatcherForQuery(queryId, listOf(historyWriterEventConsumer))
            vyne.query(
               taxiQlQuery,
               queryId = queryId,
               clientQueryId = clientQueryId,
               eventBroker = eventDispatcherForQuery
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
                  message = ""
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

   private suspend fun executeQuery(query: Query, clientQueryId: String?): QueryResponse {
      val vyne = vyneProvider.createVyne()
      val queryEventConsumer = historyWriterProvider.createEventConsumer(query.queryId, vyne.schema)

      parseFacts(query.facts, vyne.schema).forEach { (fact, factSetId) ->
         vyne.addModel(fact, factSetId)
      }

      val response = try {
         // Note: Only using the default set for the originating query,
         // but the queryEngine contains all the factSets, so we can expand this later.
         val queryId = query.queryId
         val queryContext =
            vyne.query(
               factSetIds = setOf(FactSets.DEFAULT),
               queryId = queryId,
               clientQueryId = clientQueryId,
               eventBroker = activeQueryMonitor.eventDispatcherForQuery(queryId, listOf(queryEventConsumer))
            )
         when (query.queryMode) {
            QueryMode.DISCOVER -> queryContext.find(query.expression)
            QueryMode.GATHER -> queryContext.findAll(query.expression)
            QueryMode.BUILD -> queryContext.build(query.expression)
         }
      } catch (e: SearchFailedException) {
         FailedSearchResponse(e.message!!, e.profilerOperation, query.queryId)
      }


      return QueryLifecycleEventObserver(queryEventConsumer, activeQueryMonitor)
         .responseWithQueryHistoryListener(query, response)
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