package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonView
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.ProfilerOperation
import io.osmosis.polymer.query.QueryResponse
import io.osmosis.polymer.query.SearchFailedException
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.TypeLightView
import io.polymer.spring.PolymerFactory
import io.vyne.query.Query
import io.vyne.query.QueryMode
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
 * QueryService provides a simple way to submit queries to polymer, and
 * explore the results.
 *
 * Not something you'd use in production code (your services would interact
 * with Polymer directly), but useful for spiking / demos.
 */
@RestController
class QueryService(val polymerFactory: PolymerFactory) {

   @PostMapping("/query")
   fun submitQuery(@RequestBody query: Query): QueryResponse {
      val polymer = polymerFactory.createPolymer()
      val facts = parseFacts(query.facts, polymer.schema)

      return try {
         when (query.queryMode) {
            QueryMode.DISCOVER -> polymer.query().find(query.queryString, facts)
            QueryMode.GATHER -> polymer.query().gather(query.queryString, facts)
         }
      } catch (e: SearchFailedException) {
         FailedSearchResponse(e.message!!, e.profilerOperation)
      }

   }

   private fun parseFacts(facts: Map<String, Any>, schema: Schema): Set<TypedInstance> {
      return facts.map { (typeName, attributes) ->
         TypedInstance.from(schema.type(typeName), attributes, schema)
      }.toSet()
   }
}
