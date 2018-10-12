package io.vyne.queryService

import io.vyne.models.TypedInstance
import io.vyne.query.ProfilerOperation
import io.vyne.query.QueryResponse
import io.vyne.query.SearchFailedException
import io.vyne.schemas.Schema
import io.vyne.schemas.TypeLightView
import io.vyne.spring.VyneFactory
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
 * QueryService provides a simple way to submit queries to vyne, and
 * explore the results.
 *
 * Not something you'd use in production code (your services would interact
 * with Vyne directly), but useful for spiking / demos.
 */
@RestController
class QueryService(val vyneFactory: VyneFactory) {

   @PostMapping("/query")
   fun submitQuery(@RequestBody query: Query): QueryResponse {
      val vyne = vyneFactory.createVyne()
      val facts = parseFacts(query.facts, vyne.schema)

      return try {
         when (query.queryMode) {
            QueryMode.DISCOVER -> vyne.query().find(query.queryString, facts)
            QueryMode.GATHER -> vyne.query().gather(query.queryString, facts)
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
