package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.ContextAttributes
import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.query.*
import io.vyne.queryService.csv.toCsv
import io.vyne.schemas.Schema
import io.vyne.spring.VyneProvider
import io.vyne.utils.log
import io.vyne.utils.orElse
import io.vyne.utils.timed
import io.vyne.vyneql.VyneQLQueryString
import lang.taxi.CompilationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream
import java.util.*

const val TEXT_CSV = "text/csv"
const val TEXT_CSV_UTF_8="$TEXT_CSV;charset=UTF-8"
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

   @PostMapping("/api/query", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE, TEXT_CSV])
   fun submitQuery(@RequestBody query: Query,
                   @RequestParam("resultMode", defaultValue = "SIMPLE") resultMode: ResultMode,
                   @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String
   ):ResponseEntity<StreamingResponseBody> {

      val body = StreamingResponseBody { outputStream ->
         query(query, contentType, outputStream, resultMode)
      }
      val responseContentType = when(contentType) {
         TEXT_CSV -> TEXT_CSV_UTF_8
         else -> contentType
      }
      return ResponseEntity.ok()
         .header(HttpHeaders.CONTENT_TYPE, responseContentType)
         .body(body)
   }


   @PostMapping("/api/vyneql", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE, TEXT_CSV])
   fun submitVyneQlQuery(@RequestBody query: VyneQLQueryString,
                         @RequestParam("resultMode", defaultValue = "SIMPLE") resultMode: ResultMode,
                         @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String
   ):ResponseEntity<StreamingResponseBody>  {
      val body = StreamingResponseBody {outputStream ->
         vyneQLQuery(query, resultMode, contentType, outputStream)
      }
      val responseContentType = when(contentType) {
         TEXT_CSV -> TEXT_CSV_UTF_8
         else -> contentType
      }
      return ResponseEntity.ok()
         .header(HttpHeaders.CONTENT_TYPE, responseContentType)
         .body(body)

   }


   private fun query(query: Query, contentType: String, outputStream: OutputStream, resultMode: ResultMode):MimeTypeString {
      val response = executeQuery(query)
      history.add(RestfulQueryHistoryRecord(query, response.historyRecord()))
      return serialise(response, contentType, outputStream, resultMode)
   }

   private fun vyneQLQuery(query: VyneQLQueryString, resultMode: ResultMode, contentType: String, outputStream: OutputStream):MimeTypeString {
      log().info("VyneQL query => $query")
      return timed("QueryService.submitVyneQlQuery") {
         val vyne = vyneProvider.createVyne()
         val response = try {
            vyne.query(query)
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
         val record = VyneQlQueryHistoryRecord(query, response.historyRecord())
         history.add(record)

         serialise(response, contentType, outputStream, resultMode)
      }
   }

   /**
    * Serializes the content to the outputstream, using contentType as a preferred
    * content type.
    * Returns the MediaType that was ultimately selected
    */
   private fun serialise(queryResponse: QueryResponse, contentType: String, outputStream: OutputStream, resultMode: ResultMode):MimeTypeString {
      return when (queryResponse) {
         is QueryResult -> {
            when (resultMode) {
               // If RAW result, we serialise depending on content type
               ResultMode.RAW -> {
                  return when (contentType) {
                     TEXT_CSV -> outputStream.write(toCsv(queryResponse.simpleResults, vyneProvider.createVyne().schema)).let { TEXT_CSV }
                     else -> toJson(queryResponse.simpleResults, outputStream, resultMode).let { MediaType.APPLICATION_JSON_VALUE }
                  }
               }
               // Any other result mode is json
               else -> generateResponseJson(queryResponse, outputStream, resultMode).let { MediaType.APPLICATION_JSON_VALUE }
            }
         }
         // Any Query failure is json
         else -> generateResponseJson(queryResponse, outputStream, resultMode).let { MediaType.APPLICATION_JSON_VALUE }
      }
   }

   private fun toJson(results: Map<String, Any?>, outputStream: OutputStream, resultMode: ResultMode) {
      val firstResult = results.values.firstOrNull().orElse(emptyMap<String, Any?>())
      generateResponseJson(firstResult, outputStream, resultMode)
   }

   private fun generateResponseJson(response: Any, outputStream: OutputStream, resultMode:ResultMode) {
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
            QueryMode.DISCOVER -> queryContext.find(query.expression)
            QueryMode.GATHER -> queryContext.findAll(query.expression)
            QueryMode.BUILD -> queryContext.build(query.expression)
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
