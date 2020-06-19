package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.models.TypedInstance
import io.vyne.query.*
import io.vyne.schemas.Schema
import io.vyne.schemas.TypeLightView
import io.vyne.spring.VyneFactory
import io.vyne.utils.log
import io.vyne.utils.timed
import io.vyne.vyneql.VyneQLQueryString
import lang.taxi.CompilationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*


@ResponseStatus(HttpStatus.BAD_REQUEST)
data class FailedSearchResponse(val message: String,
                                @field:JsonIgnore // this sends too much information - need to build a lightweight version
                                override val profilerOperation: ProfilerOperation?,
                                override val resultMode: ResultMode,
                                override val queryResponseId: String = UUID.randomUUID().toString()

) : QueryResponse {
   override val isFullyResolved: Boolean = false
   override fun historyRecord(): HistoryQueryResponse {
      return HistoryQueryResponse(mapOf(), listOf(), null, queryResponseId, resultMode, profilerOperation?.toDto(), listOf(), mapOf(), false)
   }
}

/**
 * QueryService provides a simple way to submit queries to vyne, and
 * explore the results.
 *
 * Not something you'd use in production code (your services would interact
 * with Vyne directly), but useful for spiking / demos.
 */
@RestController
class QueryService(val vyneFactory: VyneFactory, val history: QueryHistory) {

   @PostMapping("/query")
   fun submitQuery(@RequestBody query: Query): QueryResponse {
      val response = executeQuery(query)
      history.add(RestfulQueryHistoryRecord(query, response.historyRecord()))
      return response
   }

   @PostMapping("/vyneql")
   fun submitVyneQlQuery(@RequestBody query: VyneQLQueryString): QueryResponse {
      log().info("VyneQL query => $query")
      return timed("QueryService.submitVyneQlQuery") {
         val vyne = vyneFactory.createVyne()
         val response: QueryResponse = try {
            vyne.query(query)
         } catch (e: CompilationException) {
            FailedSearchResponse(
               message = e.message!!, // Message contains the error messages from the compiler
               profilerOperation = null,
               resultMode = ResultMode.SIMPLE
            )
         }

         history.add(VyneQlQueryHistoryRecord(query, response.historyRecord()))
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
         TypedInstance.from(schema.type(typeName), value, schema) to factSetId
      }
   }
}
