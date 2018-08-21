package io.vyne.queryService

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.ProfilerOperation
import io.osmosis.polymer.query.QueryMode
import io.osmosis.polymer.query.SearchFailedException
import io.osmosis.polymer.schemas.Schema
import io.polymer.spring.PolymerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

// TODO : facts should be QualifiedName -> TypedInstance, but need to get
// json deserialization working for that.
data class Query(val queryString: String, val facts: Map<String, Any> = emptyMap(), val queryMode: QueryMode = QueryMode.DISCOVER)

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class FailedSearchResponse(val message: String, val profilerOperation: ProfilerOperation)

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
   fun submitQuery(@RequestBody query: Query): Any {
      val polymer = polymerFactory.createPolymer()
      val facts = parseFacts(query.facts, polymer.schema)
      try {
         return when (query.queryMode) {
            QueryMode.DISCOVER -> polymer.query().find(query.queryString, facts)
            QueryMode.GATHER -> polymer.query().gather(query.queryString, facts)
         }
      } catch (e: SearchFailedException) {
         return ResponseEntity(FailedSearchResponse(e.message!!, e.profilerOperation), HttpStatus.BAD_REQUEST)
      }

   }

   private fun parseFacts(facts: Map<String, Any>, schema: Schema): Set<TypedInstance> {
      return facts.map { (typeName, attributes) ->
         TypedInstance.from(schema.type(typeName), attributes, schema)
      }.toSet()
   }
}
