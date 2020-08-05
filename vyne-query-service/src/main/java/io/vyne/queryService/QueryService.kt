package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.query.*
import io.vyne.queryService.csv.toCsv
import io.vyne.schemas.Schema
import io.vyne.spring.VyneFactory
import io.vyne.spring.VyneProvider
import io.vyne.utils.log
import io.vyne.utils.orElse
import io.vyne.utils.timed
import io.vyne.vyneql.VyneQLQueryString
import lang.taxi.CompilationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*

const val TEXT_CSV = "text/csv"

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class FailedSearchResponse(val message: String,
                                @field:JsonIgnore // this sends too much information - need to build a lightweight version
                                override val profilerOperation: ProfilerOperation?,
                                override val resultMode: ResultMode,
                                override val queryResponseId: String = UUID.randomUUID().toString()

) : QueryResponse {
   override val isFullyResolved: Boolean = false
   override fun historyRecord(): HistoryQueryResponse {
      return HistoryQueryResponse(
         fullyResolved = false,
         queryResponseId = queryResponseId,
         resultMode = resultMode,
         profilerOperation = profilerOperation?.toDto(),
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
class QueryService(val vyneProvider: VyneProvider, val history: QueryHistory) {


   @PostMapping("/api/query", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE, TEXT_CSV])
   fun submitQuery(@RequestBody query: Query,
                   @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String)
      = query(query, contentType)


   @PostMapping("/api/vyneql", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE, TEXT_CSV])
   fun submitVyneQlQuery(@RequestBody query: VyneQLQueryString,
                         @RequestParam("resultMode", defaultValue = "VERBOSE") resultMode: ResultMode,
                         @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) contentType: String)
      = vyneQLQuery(query, resultMode, contentType)


   private fun query(query: Query, contentType: String): String {
      val response = executeQuery(query)
      history.add(RestfulQueryHistoryRecord(query, response.historyRecord()))
      return serialise(response, contentType)
   }

   private fun vyneQLQuery(query: VyneQLQueryString, resultMode: ResultMode, contentType: String): QueryResponseString {
      log().info("VyneQL query => $query")
      return timed("QueryService.submitVyneQlQuery") {
         val vyne = vyneProvider.createVyne()
         val response = try {
            vyne.query(query, resultMode)
         } catch (e: CompilationException) {
            FailedSearchResponse(
               message = e.message!!, // Message contains the error messages from the compiler
               profilerOperation = null,
               resultMode = resultMode
            )
         } catch (e: SearchFailedException) {
            FailedSearchResponse(e.message!!, e.profilerOperation, resultMode)
         }
         val record = VyneQlQueryHistoryRecord(query, response.historyRecord())
         history.add(record)

         serialise(response, contentType)
      }
   }

   private fun serialise(queryResponse: QueryResponse, contentType: String): String {
      return when (queryResponse) {
         is QueryResult -> {
            when (queryResponse.resultMode) {
               // If RAW result, we serialise depending on content type
               ResultMode.RAW -> {
                  return when (contentType) {
                     TEXT_CSV -> String(toCsv(queryResponse.resultMap))
                     else -> toJson(queryResponse.resultMap)
                  }
               }
               // Any other result mode is json
               else -> generateResponseJson(queryResponse)
            }
         }
         // Any Query failure is json
         else ->  generateResponseJson(queryResponse)
      }
   }

   fun toJson(results: Map<String, Any?>): String {
      val firstResult = results.values.firstOrNull().orElse(emptyMap<String, Any?>())
      return generateResponseJson(firstResult)
   }

   fun generateResponseJson(response: Any): String {
      // We handle the serialization here, and return a string, rather than
      // letting Spring handle it.
      // This is because the LineageGraphSerializationModule() is stateful, and
      // shares references during serialization.  Therefore, it's not threadsafe, so
      // we create an instance per response.
      return jacksonObjectMapper()
         .registerModule(LineageGraphSerializationModule())
         .writerWithDefaultPrettyPrinter()
         .writeValueAsString(response)
   }

   private fun executeQuery(query: Query): QueryResponse {
      val vyne = vyneProvider.createVyne()

      parseFacts(query.facts, vyne.schema).forEach { (fact, factSetId) ->
         vyne.addModel(fact, factSetId)
      }

      return try {
         // Note: Only using the default set for the originating query,
         // but the queryEngine contains all the factSets, so we can expand this later.
         val queryContext = vyne.query(factSetIds = setOf(FactSets.DEFAULT), resultMode = query.resultMode)
         when (query.queryMode) {
            QueryMode.DISCOVER -> queryContext.find(query.expression)
            QueryMode.GATHER -> queryContext.findAll(query.expression)
            QueryMode.BUILD -> queryContext.build(query.expression)
         }
      } catch (e: SearchFailedException) {
         FailedSearchResponse(e.message!!, e.profilerOperation, query.resultMode)
      }
   }

   private fun parseFacts(facts: List<Fact>, schema: Schema): List<Pair<TypedInstance, FactSetId>> {

      return facts.map { (typeName, value, factSetId) ->
         TypedInstance.from(schema.type(typeName), value, schema, source = Provided) to factSetId
      }
   }
}
