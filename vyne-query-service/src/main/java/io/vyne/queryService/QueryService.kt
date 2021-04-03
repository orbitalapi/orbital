package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.ContextAttributes
import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.models.Provided
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.query.*
import io.vyne.queryService.csv.toCsv
import io.vyne.queryService.security.VyneUser
import io.vyne.queryService.security.facts
import io.vyne.queryService.security.toVyneUser
import io.vyne.schemas.Schema
import io.vyne.spring.VyneProvider
import io.vyne.utils.log
import io.vyne.utils.orElse
import io.vyne.vyneql.VyneQLQueryString
import lang.taxi.CompilationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import java.io.OutputStream
import io.vyne.query.history.RestfulQueryHistoryRecord
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayOutputStream
import java.util.UUID

const val TEXT_CSV = "text/csv"
const val TEXT_CSV_UTF_8 = "$TEXT_CSV;charset=UTF-8"
private typealias MimeTypeString = String

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class FailedSearchResponse(val message: String,
                                @field:JsonIgnore // this sends too much information - need to build a lightweight version
                                override val profilerOperation: ProfilerOperation?,
                                override val queryResponseId: String = UUID.randomUUID().toString(),
                                val results: Map<String, Any?> = mapOf()

) : QueryResponse {
   override val responseStatus: QueryResponse.ResponseStatus = QueryResponse.ResponseStatus.ERROR
   override val isFullyResolved: Boolean = false
   override fun historyRecord(): HistoryQueryResponse {
      return HistoryQueryResponse(
         fullyResolved = false,
         queryResponseId = queryResponseId,
         profilerOperation = profilerOperation?.toDto(),
         responseStatus = this.responseStatus,
         error = message)
   }
}

/**
 * We have to do some funky serialization for QueryResult,
 * so controller methods are marked to return the Json directly, rather
 * than allow the default Jackson serialization to take hold
 */
typealias QueryResponseString = String

/**
 * QueryService provides a simple way to submit queries to vyne, and
 * explore the results.
 *
 * Not something you'd use in production code (your services would interact
 * with Vyne directly), but useful for spiking / demos.
 */
@RestController
class QueryService(val vyneProvider: VyneProvider, val history: QueryHistory, val objectMapper: ObjectMapper) {

   val typedInstanceConverter:TypedInstanceConverter = TypedInstanceConverter(RawObjectMapper)

   suspend fun monitored(queryId: String, block: () -> QueryResponse): QueryResponse {
      return runBlocking { block.invoke() }
   }

   @PostMapping("/api/query", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE, TEXT_CSV])
   fun submitQuery(@RequestBody query: Query,
                   @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
                   @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String
   ): ResponseEntity<String> {

      //TODO - StreamingResponseBody does not work with this spring/webflux configuration... why????
      val outputStream = ByteArrayOutputStream()
      runBlocking {
         query(query, contentType, outputStream, resultMode)
      }

      val responseContentType = when (contentType) {
         TEXT_CSV -> TEXT_CSV_UTF_8
         else -> contentType
      }
      return ResponseEntity.ok()
         .header(HttpHeaders.CONTENT_TYPE, responseContentType)
         .body(outputStream.toString())
   }


   @PostMapping("/api/vyneql", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE, TEXT_CSV])
   fun submitVyneQlQuery(@RequestBody query: VyneQLQueryString,
                         @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
                         @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String,
                         auth: Authentication? = null
   ): ResponseEntity<String> {
      val user = auth?.toVyneUser()
      //TODO - StreamingResponseBody does not work with this spring/webflux configuration... why????
      val outputStream = ByteArrayOutputStream()

      runBlocking {
         val response = vyneQLQuery(query, user)
         serialise(response, contentType, outputStream, resultMode)
      }

      val responseContentType = when (contentType) {
         TEXT_CSV -> TEXT_CSV_UTF_8
         else -> contentType
      }
      return ResponseEntity.ok()
         .header(HttpHeaders.CONTENT_TYPE, responseContentType)
         .body(outputStream.toString())

   }

   @PostMapping("/api/vyneql", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
   suspend fun submitVyneQlQueryStreamingResponse(@RequestBody query: VyneQLQueryString,
                                          @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
                                          @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String,
                                          auth: Authentication? = null
   ): Flow<Any?>? {
      val user = auth?.toVyneUser()
      val queryResponse = vyneQLQuery(query, user) as QueryResult
      return queryResponse.results?.map { typedInstanceConverter.convert(it) }
   }


   private suspend fun query(query: Query, contentType: String, outputStream: OutputStream, resultMode: ResultMode): MimeTypeString {
      val response = executeQuery(query)
      history.add { RestfulQueryHistoryRecord(query, response.historyRecord()) }
      return serialise(response, contentType, outputStream, resultMode)
   }

   private suspend fun vyneQLQuery(query: VyneQLQueryString,
                                   vyneUser: VyneUser? = null
   ): QueryResponse = monitored(query) {
      log().info("VyneQL query => $query")
      //return timed("QueryService.submitVyneQlQuery") {
      val vyne = vyneProvider.createVyne(vyneUser.facts())

      val response = try {
         runBlocking { vyne.query(query) }
      } catch (e: CompilationException) {
         FailedSearchResponse(
            message = e.message!!, // Message contains the error messages from the compiler
            profilerOperation = null
         )
      } catch (e: SearchFailedException) {
         FailedSearchResponse(e.message!!, e.profilerOperation)
      } catch (e: NotImplementedError) {
         // happens when Schema is empty
         FailedSearchResponse(e.message!!, null)
      }
      //val recordProvider = {
      //   VyneQlQueryHistoryRecord(query, response.historyRecord())
      //}
      //history.add(recordProvider)

      response
      //}

   }



   /**
    * Serializes the content to the outputstream, using contentType as a preferred
    * content type.
    * Returns the MediaType that was ultimately selected
    */
   private suspend fun serialise(queryResponse: QueryResponse, contentType: String, outputStream: OutputStream, resultMode: ResultMode): MimeTypeString {
      return when (queryResponse) {
         is QueryResult -> {
            when (resultMode) {
               // If RAW result, we serialise depending on content type
               ResultMode.RAW -> {
                  return when (contentType) {
                     TEXT_CSV -> outputStream.write(
                        toCsv(
                           mapOf(queryResponse.type.type.name.parameterizedName to queryResponse.simpleResults?.toList()?.map { typedInstanceConverter.convert(it)}), vyneProvider.createVyne().schema
                        )
                     ).let { TEXT_CSV }


                     else -> toJson(mapOf(queryResponse.type.toString() to queryResponse.simpleResults?.toList()?.map { typedInstanceConverter.convert(it)}), outputStream, resultMode).let { MediaType.APPLICATION_JSON_VALUE }
                  }
               }
               // Any other result mode is json
               else -> {
                  generateResponseJson( mapOf(
                     "results" to mapOf(queryResponse.type.type.name.parameterizedName to queryResponse.results?.toList()?.map { typedInstanceConverter.convert(it)} ),
                     "queryResponseId" to UUID.randomUUID().toString(),
                     "responseStatus" to "COMPLETED",
                     "fullyResolved" to true,
                     "vyneCost" to 0,
                     "truncated" to false,
                     "unmatchedNodes" to emptyList<QuerySpecTypeNode>()
                  ),
                     outputStream,
                     resultMode
                  ).let { MediaType.APPLICATION_JSON_VALUE }
               }
            }
         }
         // Any Query failure is json
         else -> {
            generateResponseJson(queryResponse, outputStream, resultMode).let { MediaType.APPLICATION_JSON_VALUE }
         }
      }
   }

   private fun toJson(results: Map<String, Any?>, outputStream: OutputStream, resultMode: ResultMode) {
      val firstResult = results.values.firstOrNull().orElse(emptyMap<String, Any?>())
      generateResponseJson(firstResult, outputStream, resultMode)
   }

   private fun generateResponseJson(response: Any?, outputStream: OutputStream, resultMode: ResultMode) {
      // We handle the serialization here, and return a string, rather than
      // letting Spring handle it.
      // This is because the LineageGraphSerializationModule() is stateful, and
      // shares references during serialization.  Therefore, it's not threadsafe, so
      // we create an instance per response.
      objectMapper
         //        .copy()
//         .registerModule(LineageGraphSerializationModule())
            .writerWithDefaultPrettyPrinter()
            .with(ContextAttributes.getEmpty()
            .withSharedAttribute(ResultMode::class, resultMode)
         )
         .writeValue(outputStream, response)
   }

   private fun executeQuery(query: Query): QueryResponse {
      val vyne = vyneProvider.createVyne()

      parseFacts(query.facts, vyne.schema).forEach { (fact, factSetId) ->
         vyne.addModel(fact, factSetId)
      }

      return try {
         // Note: Only using the default set for the originating query,
         // but the queryEngine contains all the factSets, so we can expand this later.
         val queryContext = vyne.query(factSetIds = setOf(FactSets.DEFAULT))
         when (query.queryMode) {
            QueryMode.DISCOVER ->  runBlocking {queryContext.find(query.expression) }
            QueryMode.GATHER ->  runBlocking {queryContext.findAll(query.expression) }
            QueryMode.BUILD ->  runBlocking {queryContext.build(query.expression) }
         }
      } catch (e: SearchFailedException) {
         FailedSearchResponse(e.message!!, e.profilerOperation)
      }
   }

   private fun parseFacts(facts: List<Fact>, schema: Schema): List<Pair<TypedInstance, FactSetId>> {

      return facts.map { (typeName, value, factSetId) ->
         TypedInstance.from(schema.type(typeName), value, schema, source = Provided) to factSetId
      }
   }
}
