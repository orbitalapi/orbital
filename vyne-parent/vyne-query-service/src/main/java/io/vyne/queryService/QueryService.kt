package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.FactSetId
import io.vyne.FactSets
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
import java.util.*


@ResponseStatus(HttpStatus.BAD_REQUEST)
data class FailedSearchResponse(val message: String,
                                @field:JsonIgnore // this sends too much information - need to build a lightweight version
                                override val profilerOperation: ProfilerOperation,
                                override val queryResponseId: String = UUID.randomUUID().toString()
) : QueryResponse {
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
         // Note: Only using the default set for the originating query,
         // but the queryEngine contains all the factSets, so we can expand this later.
         when (query.queryMode) {
            QueryMode.DISCOVER -> vyne.query(setOf(FactSets.DEFAULT)).find(query.queryString)
            QueryMode.GATHER -> vyne.query(setOf(FactSets.DEFAULT)).gather(query.queryString)
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
