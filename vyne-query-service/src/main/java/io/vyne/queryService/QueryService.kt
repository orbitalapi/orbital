package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.*
import io.vyne.queryService.csv.toCsv
import io.vyne.queryService.history.QueryEventObserver
import io.vyne.queryService.history.db.QueryHistoryDbWriter
import io.vyne.queryService.security.VyneUser
import io.vyne.queryService.security.facts
import io.vyne.queryService.security.toVyneUser
import io.vyne.schemas.Schema
import io.vyne.spring.VyneProvider
import io.vyne.utils.log
import io.vyne.vyneql.TaxiQlQueryString
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import lang.taxi.CompilationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

const val TEXT_CSV = "text/csv"
const val TEXT_CSV_UTF_8 = "$TEXT_CSV;charset=UTF-8"
private typealias MimeTypeString = String

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class FailedSearchResponse(
   val message: String,
   @field:JsonIgnore // this sends too much information - need to build a lightweight version
   override val profilerOperation: ProfilerOperation?,
   override val queryResponseId: String = UUID.randomUUID().toString(),
   val results: Map<String, Any?> = mapOf(),
   override val clientQueryId: String? = null

) : QueryResponse {
   override val responseStatus: QueryResponse.ResponseStatus = QueryResponse.ResponseStatus.ERROR
   override val isFullyResolved: Boolean = false
   override fun historyRecord(): HistoryQueryResponse {
      return HistoryQueryResponse(
         fullyResolved = false,
         queryResponseId = queryResponseId,
         profilerOperation = profilerOperation?.toDto(),
         responseStatus = this.responseStatus,
         error = message
      )
   }
}

/**
 * We have to do some funky serialization for QueryResult,
 * so controller methods are marked to return the Json directly, rather
 * than allow the default Jackson serialization to take hold
 */
typealias QueryResponseString = String

/**
 * Main entry point for submitting queries to Vyne.
 */
@RestController
class QueryService(
   val vyneProvider: VyneProvider,
   val historyDbWriter: QueryHistoryDbWriter,
   val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   val queryMonitor: QueryMonitor
) {

   @PostMapping(
      "/api/query",
      consumes = [MediaType.APPLICATION_JSON_VALUE],
      produces = [MediaType.APPLICATION_JSON_VALUE, TEXT_CSV]
   )
   suspend fun submitQuery(
      @RequestBody query: Query,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String,
   ): ResponseEntity<Flow<Any>> {

      val queryResult = executeQuery(query)
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
         .body(convertToExpectedResult(queryResult, resultMode, contentType))
   }

   private fun convertToExpectedResult(
      queryResult: QueryResponse,
      resultMode: ResultMode,
      contentType: String
   ): Flow<Any> {
      return when (queryResult) {
         is QueryResult -> convertToExpectedResult(queryResult, resultMode, contentType)
         is FailedSearchResponse -> convertToExpectedResult(queryResult, contentType)
         else -> error("Received unknown type of QueryResponse: ${queryResult::class.simpleName}")
      }
   }

   private fun convertToExpectedResult(
      queryResult: QueryResult,
      resultMode: ResultMode,
      contentType: String
   ): Flow<Any> {
      return when (contentType) {
         TEXT_CSV -> toCsv(queryResult.results, vyneProvider.createVyne().schema)
         // Default everything else to JSON
         else -> {
            val serializer = resultMode.buildSerializer(queryResult)
            queryResult.results
               .flatMapMerge { typedInstance ->
                  // This is a smell.
                  // I've noticed that when projecting, in this point of the code
                  // we get individual typed instances.
                  // However, if we're not projecting, we get a single
                  // typed collection.
                  // This meas that the shape of the response (array vs single)
                  // varies based on the query, which is incorrect.
                  // Therefore, unwrap collections here.
                  // This smells, because it could be indicative of a problem
                  // higher in the stack.
                  if (typedInstance is TypedCollection) {
                     typedInstance.map { serializer.serialize(it) }
                  } else {
                     listOf(serializer.serialize(typedInstance))
                  }.filterNotNull()
                     .asFlow()

               }
               .filterNotNull()
         }
      }
   }

   private fun convertToExpectedResult(
      failure: FailedSearchResponse,
      contentType: String
   ): Flow<Any> {
      return when (contentType) {
         TEXT_CSV -> flowOf(failure.message)
         // Assume everything else is JSON.  Return the entity, and let
         // Spring / Jackson take care of the serialzation.
         else -> flowOf(failure)
      }
   }


   suspend fun monitored(
      query: TaxiQlQueryString,
      clientQueryId: String?,
      vyneUser: VyneUser?,
      block: suspend () -> QueryResponse
   ): QueryResponse = GlobalScope.run {
      queryMonitor.reportStart()
      val ret = block.invoke()
      queryMonitor.reportComplete()
      return ret
   }


   @PostMapping(
      "/api/vyneql",
      consumes = [MediaType.APPLICATION_JSON_VALUE],
      produces = [MediaType.APPLICATION_JSON_VALUE, TEXT_CSV]
   )
   suspend fun submitVyneQlQuery(
      @RequestBody query: TaxiQlQueryString,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode = ResultMode.RAW,
      @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String = MediaType.APPLICATION_JSON_VALUE,
      auth: Authentication? = null,
      @RequestParam("clientQueryId", required = false) clientQueryId: String? = null
   ): ResponseEntity<Flow<Any>> {
      val user = auth?.toVyneUser()
      val response = vyneQLQuery(query, user, clientQueryId = clientQueryId)
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
      consumes = [MediaType.APPLICATION_JSON_VALUE],
      produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
   )
   suspend fun submitVyneQlQueryStreamingResponse(
      @RequestBody query: TaxiQlQueryString,
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
      @RequestParam("query") query: TaxiQlQueryString,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String,
      auth: Authentication? = null,
      @RequestParam("clientQueryId", required = false) clientQueryId: String? = null
   ): Flow<Any?> {
      val user = auth?.toVyneUser()
      val queryResponse = vyneQLQuery(query, user, clientQueryId)
      return when (queryResponse) {
         is FailedSearchResponse -> flowOf(queryResponse)
         is QueryResult -> {
            val resultSerializer = resultMode.buildSerializer(queryResponse)
            queryResponse.results?.map { resultSerializer.serialize(it) }
         }

         else -> error("Unhandled type of QueryResponse - received ${queryResponse::class.simpleName}")
      } ?: emptyFlow()
   }


   private suspend fun vyneQLQuery(
      query: TaxiQlQueryString,
      vyneUser: VyneUser? = null,
      clientQueryId: String?
   ): QueryResponse = monitored(query, clientQueryId, vyneUser) {
      log().info("VyneQL query => $query")
      val vyne = vyneProvider.createVyne(vyneUser.facts())
      val response = try {
         vyne.query(query, clientQueryId = clientQueryId)
      } catch (e: CompilationException) {
         log().info("The query failed compilation: ${e.message}")
         FailedSearchResponse(
            message = e.message!!, // Message contains the error messages from the compiler
            profilerOperation = null,
            clientQueryId = clientQueryId
         )
      } catch (e: SearchFailedException) {
         FailedSearchResponse(e.message!!, e.profilerOperation, clientQueryId = clientQueryId)
      } catch (e: NotImplementedError) {
         // happens when Schema is empty
         FailedSearchResponse(e.message!!, null, clientQueryId = clientQueryId)
      }

      QueryEventObserver(historyDbWriter)
         .responseWithQueryHistoryListener(query, response)
   }

//
//   /**
//    * Serializes the content to the outputstream, using contentType as a preferred
//    * content type.
//    * Returns the MediaType that was ultimately selected
//    */
//   private suspend fun serialise(
//      queryResponse: QueryResponse,
//      contentType: String,
//      outputStream: OutputStream,
//      resultMode: ResultMode
//   ): MimeTypeString {
//      return when (queryResponse) {
//         is QueryResult -> {
//            when (resultMode) {
//               // If RAW result, we serialise depending on content type
//               ResultMode.RAW -> {
//                  return when (contentType) {
//                     TEXT_CSV -> outputStream.write(
//                        toCsv(
//                           mapOf(
//                              queryResponse.querySpec.type.name.parameterizedName to queryResponse.rawResults.toList()
//                                 ?.map { typedInstanceConverter.convert(it) }), vyneProvider.createVyne().schema
//                        )
//                     ).let { TEXT_CSV }
//
//
//                     else -> toJson(
//                        mapOf(
//                           queryResponse.querySpec.toString() to queryResponse.rawResults?.toList()
//                              ?.map { typedInstanceConverter.convert(it) }), outputStream, resultMode
//                     ).let { MediaType.APPLICATION_JSON_VALUE }
//                  }
//               }
//               // Any other result mode is json
//               else -> {
//                  generateResponseJson(
//                     mapOf(
//                        "results" to mapOf(
//                           queryResponse.querySpec.type.name.parameterizedName to queryResponse.results?.toList()
//                              ?.map { typedInstanceConverter.convert(it) }),
//                        "queryResponseId" to UUID.randomUUID().toString(),
//                        "responseStatus" to "COMPLETED",
//                        "fullyResolved" to true,
//                        "vyneCost" to 0,
//                        "truncated" to false,
//                        "unmatchedNodes" to emptyList<QuerySpecTypeNode>()
//                     ),
//                     outputStream,
//                     resultMode
//                  ).let { MediaType.APPLICATION_JSON_VALUE }
//               }
//            }
//         }
//         // Any Query failure is json
//         else -> {
//            generateResponseJson(queryResponse, outputStream, resultMode).let { MediaType.APPLICATION_JSON_VALUE }
//         }
//      }
//   }
//
//   private fun toJson(results: Map<String, Any?>, outputStream: OutputStream, resultMode: ResultMode) {
//      val firstResult = results.values.firstOrNull().orElse(emptyMap<String, Any?>())
//      generateResponseJson(firstResult, outputStream, resultMode)
//   }
//
//   private fun generateResponseJson(response: Any?, outputStream: OutputStream, resultMode: ResultMode) {
//      // We handle the serialization here, and return a string, rather than
//      // letting Spring handle it.
//      // This is because the LineageGraphSerializationModule() is stateful, and
//      // shares references during serialization.  Therefore, it's not threadsafe, so
//      // we create an instance per response.
//      objectMapper
//         //        .copy()
////         .registerModule(LineageGraphSerializationModule())
//         .writerWithDefaultPrettyPrinter()
//         .with(
//            ContextAttributes.getEmpty()
//               .withSharedAttribute(ResultMode::class, resultMode)
//         )
//         .createGenerator(outputStream)
//         .writeValue(outputStream, response)
//   }

   private suspend fun executeQuery(query: Query): QueryResponse {
      val vyne = vyneProvider.createVyne()

      parseFacts(query.facts, vyne.schema).forEach { (fact, factSetId) ->
         vyne.addModel(fact, factSetId)
      }

      val response = try {
         // Note: Only using the default set for the originating query,
         // but the queryEngine contains all the factSets, so we can expand this later.
         val queryContext = vyne.query(factSetIds = setOf(FactSets.DEFAULT))
         when (query.queryMode) {
            QueryMode.DISCOVER -> queryContext.find(query.expression)
            QueryMode.GATHER -> queryContext.findAll(query.expression)
            QueryMode.BUILD -> queryContext.build(query.expression)
         }
      } catch (e: SearchFailedException) {
         FailedSearchResponse(e.message!!, e.profilerOperation)
      }

      return QueryEventObserver(historyDbWriter)
         .responseWithQueryHistoryListener(query, response)
   }

   private fun parseFacts(facts: List<Fact>, schema: Schema): List<Pair<TypedInstance, FactSetId>> {

      return facts.map { (typeName, value, factSetId) ->
         TypedInstance.from(schema.type(typeName), value, schema, source = Provided) to factSetId
      }
   }
}
