package io.vyne.queryService

import io.vyne.FactSetId
import io.vyne.models.TypedInstance
import io.vyne.query.*
import io.vyne.schemas.Schema
import io.vyne.schemas.TypeLightView
import io.vyne.spring.VyneFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController


@ResponseStatus(HttpStatus.BAD_REQUEST)
data class FailedSearchResponse(val message: String, override val profilerOperation: ProfilerOperation) : QueryResponse {
   override val isFullyResolved: Boolean = false
}

interface SchemaLightView : TypeLightView

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
      history.add(QueryHistoryRecord(query, response))
      return response
   }

   private fun executeQuery(query: Query): QueryResponse {
      val vyne = vyneFactory.createVyne()
      val facts = parseFacts(query.facts, vyne.schema)

      facts.forEach { (fact, factSetId) ->
         vyne.addModel(fact, factSetId)
      }

      return try {
         when (query.queryMode) {
            QueryMode.DISCOVER -> vyne.query().find(query.queryString)
            QueryMode.GATHER -> vyne.query().gather(query.queryString)
         }
      } catch (e: SearchFailedException) {
         FailedSearchResponse(e.message!!, e.profilerOperation)
      }
   }

   private fun parseFacts(facts: List<Fact>, schema: Schema): List<Pair<TypedInstance, FactSetId>> {

      return facts.map { (typeName, value, factSetId) ->
         TypedInstance.from(schema.type(typeName), value, schema) to factSetId
      }
   }
}
