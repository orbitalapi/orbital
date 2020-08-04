package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.query.*
import io.vyne.schemas.Schema
import io.vyne.spring.VyneFactory
import io.vyne.utils.log
import io.vyne.utils.timed
import io.vyne.vyneql.VyneQLQueryString
import lang.taxi.CompilationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


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
typealias QueryResponseJson = String

/**
 * QueryService provides a simple way to submit queries to vyne, and
 * explore the results.
 *
 * Not something you'd use in production code (your services would interact
 * with Vyne directly), but useful for spiking / demos.
 */
@RestController
class QueryService(val vyneFactory: VyneFactory, val history: QueryHistory) {

   @PostMapping("/api/query")
   fun submitQuery(@RequestBody query: Query): QueryResponseJson {
      val response = executeQuery(query)

      history.add(RestfulQueryHistoryRecord(query, response.historyRecord()))

      return generateResponseJson(response)
   }

   private fun generateResponseJson(response: QueryResponse): String {
      // We handle the serialization here, and return a string, rather than
      // letting Spring handle it.
      // This is because the LineageGraphSerializationModule() is stateful, and
      // shares references during serialization.  Therefore, it's not threadsafe, so
      // we create an instance per response.
      val json = jacksonObjectMapper()
         .registerModule(LineageGraphSerializationModule())
         .writerWithDefaultPrettyPrinter()
         .writeValueAsString(response)
      return json
   }

   @PostMapping("/api/vyneql")
   fun submitVyneQlQuery(@RequestBody query: VyneQLQueryString,
                         @RequestParam("resultMode", defaultValue = "VERBOSE") resultMode: ResultMode): QueryResponseJson {
      val response = doVyneQlQuery(query, resultMode)
      return generateResponseJson(response) // consider returning record here
   }

   internal fun doVyneQlQuery(query: VyneQLQueryString, resultMode: ResultMode): QueryResponse {
      log().info("VyneQL query => $query")
      return timed("QueryService.submitVyneQlQuery") {
         val vyne = vyneFactory.createVyne()
         val response: QueryResponse = try {
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
         response
      }
   }

   private fun executeQuery(query: Query): QueryResponse {
      val vyne = vyneFactory.createVyne()
      val facts = parseFacts(query.facts, vyne.schema)

      facts.forEach { (fact, factSetId) ->
         vyne.addModel(fact, factSetId)
      }

      val response = try {
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
      return response
   }

   private fun parseFacts(facts: List<Fact>, schema: Schema): List<Pair<TypedInstance, FactSetId>> {

      return facts.map { (typeName, value, factSetId) ->
         TypedInstance.from(schema.type(typeName), value, schema, source = Provided) to factSetId
      }
   }
}
