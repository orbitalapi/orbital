package io.vyne.queryService.query

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.Vyne
import io.vyne.history.QueryEventObserver
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.query.Fact
import io.vyne.query.FailedQueryResponse
import io.vyne.query.HistoryEventConsumerProvider
import io.vyne.query.ProfilerOperation
import io.vyne.query.Query
import io.vyne.query.QueryContextEventBroker
import io.vyne.query.QueryMode
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.query.ResultMode
import io.vyne.query.SearchFailedException
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.queryService.ErrorType
import io.vyne.queryService.security.VyneUser
import io.vyne.queryService.security.facts
import io.vyne.queryService.security.toVyneUser
import io.vyne.schemas.Schema
import io.vyne.spring.VyneProvider
import io.vyne.utils.log
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import lang.taxi.types.TaxiQLQueryString
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

const val TEXT_CSV = "text/csv"
const val TEXT_CSV_UTF_8 = "$TEXT_CSV;charset=UTF-8"
private typealias MimeTypeString = String

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class FailedSearchResponse(
   override val message: String,
   @field:JsonIgnore // this sends too much information - need to build a lightweight version
   override val profilerOperation: ProfilerOperation?,
   override val queryId: String,
   val results: Map<String, Any?> = mapOf(),
   override val clientQueryId: String? = null,
   override val responseType: String? = null


   ) : FailedQueryResponse {
   override val queryResponseId: String = queryId
}

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
   val vyneProvider: VyneProvider,
   val historyWriterProvider: HistoryEventConsumerProvider,
   val objectMapper: ObjectMapper,
   val activeQueryMonitor: ActiveQueryMonitor,
   val metricsEventConsumer: MetricsEventConsumer
) {


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
      return queryResultToResponseEntity(queryResult, resultMode, contentType)
   }

   private fun queryResultToResponseEntity(
      queryResult: QueryResponse,
      resultMode: ResultMode,
      contentType: String
   ): ResponseEntity<Flow<Any>> {
      val httpStatus = when (queryResult) {
         is QueryResult -> HttpStatus.OK
         is FailedSearchResponse -> HttpStatus.BAD_REQUEST
         else -> error("Unknown type of QueryResponse received:  ${queryResult::class.simpleName}")
      }
      return ResponseEntity.status(httpStatus)
         .header("x-vyne-query-id", queryResult.queryId)
         .header("x-vyne-client-query-id", queryResult.clientQueryId)
         .body(convertToExpectedResult(queryResult, resultMode, contentType))
   }

   private fun convertToExpectedResult(
      queryResult: QueryResponse,
      resultMode: ResultMode,
      contentType: String
   ): Flow<Any> {
      return queryResult.convertToSerializedContent(resultMode, contentType)
   }

   suspend fun monitored(
      query: TaxiQLQueryString,
      clientQueryId: String?,
      queryId: String, vyneUser: VyneUser?,
      block: suspend () -> QueryResponse
   ): QueryResponse {

      activeQueryMonitor.reportStart(queryId, clientQueryId, query)
      return block.invoke()
   }

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
      val response = vyneQLQuery(query, user, clientQueryId = clientQueryId, queryId = UUID.randomUUID().toString())
      return queryResultToResponseEntity(response, resultMode, contentType) as ResponseEntity<Flow<String>>

   }

   @PostMapping(
      value = ["/api/vyneql", "/api/taxiql"],
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "application/taxiql"],
      produces = [MediaType.APPLICATION_JSON_VALUE]
   )
   suspend fun submitVyneQlQuery(
      @RequestBody query: TaxiQLQueryString,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode = ResultMode.RAW,
      @RequestHeader(
         value = "Accept",
         defaultValue = MediaType.APPLICATION_JSON_VALUE
      ) contentType: String = MediaType.APPLICATION_JSON_VALUE,
      auth: Authentication? = null,
      @RequestParam("clientQueryId", required = false) clientQueryId: String? = null
   ): ResponseEntity<Flow<Any>> {
      val user = auth?.toVyneUser()
      val response = vyneQLQuery(
         query,
         user,
         clientQueryId = clientQueryId,
         queryId = clientQueryId ?: UUID.randomUUID().toString()
      )
      return queryResultToResponseEntity(response, resultMode, contentType)
   }

   /**
    * Endpoint for submitting a TaxiQL query, and receiving an event stream back.
    * Note that POST is used here for backwards compatability with existing approaches,
    * however browsers are unable to issue requests using SSE on anything other than a GET.
    *
    * Also, this endpoint is exposed under both /vyneql (legacy) and /taxiql (renamed).
    */
   @PostMapping(
      value = ["/api/vyneql", "/api/taxiql"],
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "application/taxiql"],
      produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
   )
   suspend fun submitVyneQlQueryStreamingResponse(
      @RequestBody query: TaxiQLQueryString,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String,
      auth: Authentication? = null,
      @RequestParam("clientQueryId", required = false) clientQueryId: String? = null
   ): Flow<Any?> {
      return getVyneQlQueryStreamingResponse(query, resultMode, contentType, auth, clientQueryId)
   }


   /**
    * Endpoint for submitting a TaxiQL query, and receiving an event stream back.
    * Browsers cannot submit POST requests for SSE responses (only GET), hence having the query in the queryString
    *
    * Also, this endpoint is exposed under both /vyneql (legacy) and /taxiql (renamed).
    */
   @GetMapping(value = ["/api/vyneql", "/api/taxiql"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
   suspend fun getVyneQlQueryStreamingResponse(
      @RequestParam("query") query: TaxiQLQueryString,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String,
      auth: Authentication? = null,
      @RequestParam("clientQueryId", required = false) clientQueryId: String? = null
   ): Flow<Any?> {
      val user = auth?.toVyneUser()
      val queryId = UUID.randomUUID().toString()
      val queryResponse = vyneQLQuery(query, user, clientQueryId, queryId)

      return when (queryResponse) {
         is FailedSearchResponse -> flowOf(queryResponse)
         is QueryResult -> {

            val resultSerializer = resultMode.buildSerializer(queryResponse)
            queryResponse.results
               .catch { throwable ->
                  when (throwable) {
                     is SearchFailedException -> {
                        logger.warn { "Search failed with a SearchFailedException. ${throwable.message!!}" }
                     }
                     else -> {
                        logger.error { "Search failed with an unexpected exception of type: ${throwable::class.simpleName}.  ${throwable.message ?: "No message provided"}" }
                     }
                  }
                  emit(ErrorType.error(throwable.message ?: "No message provided"))
                  //throw throwable
               }
               .map {
                  resultSerializer.serialize(it)
               }
         }
         else -> error("Unhandled type of QueryResponse - received ${queryResponse::class.simpleName}")
      }
   }

   private suspend fun vyneQLQuery(
      query: TaxiQLQueryString,
      vyneUser: VyneUser? = null,
      clientQueryId: String?,
      queryId: String
   ): QueryResponse = monitored(query = query, clientQueryId = clientQueryId, queryId = queryId, vyneUser = vyneUser) {
      logger.info { "[$queryId] $query" }
      val vyne = vyneProvider.createVyne(vyneUser.facts())
      val historyWriterEventConsumer = historyWriterProvider.createEventConsumer(queryId)
      val response = try {
         val eventDispatcherForQuery =
            activeQueryMonitor.eventDispatcherForQuery(queryId, listOf(historyWriterEventConsumer))
         vyne.query(query, queryId = queryId, clientQueryId = clientQueryId, eventBroker = eventDispatcherForQuery)
      } catch (e: lang.taxi.CompilationException) {
         log().info("The query failed compilation: ${e.message}")
         FailedSearchResponse(
            message = e.message!!, // Message contains the error messages from the compiler
            profilerOperation = null,
            clientQueryId = clientQueryId,
            queryId = queryId
         )
      } catch (e: SearchFailedException) {
         FailedSearchResponse(e.message!!, e.profilerOperation, queryId = queryId)

      } catch (e: NotImplementedError) {
         // happens when Schema is empty
         FailedSearchResponse(e.message!!, null, queryId = queryId)
      }

      QueryEventObserver(historyWriterEventConsumer, activeQueryMonitor, metricsEventConsumer)
         .responseWithQueryHistoryListener(query, response)
   }

   suspend fun doVyneMonitoredWork(
      vyneUser: VyneUser? = null,
      schema: Schema? = null,
      queryCallback: suspend (Vyne, QueryContextEventBroker) -> QueryResponse
   ): QueryResponse {
      val queryId: String = UUID.randomUUID().toString()
      val vyne = if (schema != null) vyneProvider.createVyne(
         vyneUser.facts(),
         schema
      ) else vyneProvider.createVyne(vyneUser.facts())

      val historyWriterEventConsumer = historyWriterProvider.createEventConsumer(queryId)
      val eventDispatcherForQuery =
         activeQueryMonitor.eventDispatcherForQuery(queryId, listOf(historyWriterEventConsumer))

      val queryResponse = queryCallback(vyne, eventDispatcherForQuery)
      return QueryEventObserver(historyWriterEventConsumer, activeQueryMonitor, metricsEventConsumer)
         .responseWithQueryHistoryListener("Adhoc query", queryResponse)
   }

   private suspend fun executeQuery(query: Query, clientQueryId: String?): QueryResponse {
      val vyne = vyneProvider.createVyne()
      val queryEventConsumer = historyWriterProvider.createEventConsumer(query.queryId)

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

      //return response

      return QueryEventObserver(queryEventConsumer, activeQueryMonitor, metricsEventConsumer)
         .responseWithQueryHistoryListener(query, response)
   }

   private fun parseFacts(facts: List<Fact>, schema: Schema): List<Pair<TypedInstance, FactSetId>> {

      return facts.map { (typeName, value, factSetId) ->
         TypedInstance.from(schema.type(typeName), value, schema, source = Provided) to factSetId
      }
   }
}

