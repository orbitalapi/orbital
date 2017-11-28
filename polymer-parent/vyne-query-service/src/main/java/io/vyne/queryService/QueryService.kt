package io.vyne.queryService

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.schemas.Schema
import io.polymer.spring.PolymerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

// TODO : facts should be QualifiedName -> TypedInstance, but need to get
// json deserialization working for that.
data class Query(val queryString:String, val facts:Map<String,Any> = emptyMap())
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
   fun submitQuery(@RequestBody query: Query):Any {
      val polymer = polymerFactory.createPolymer()
      val facts = parseFacts(query.facts, polymer.schema)
      return polymer.query().find(query.queryString,facts)
   }

   private fun parseFacts(facts: Map<String, Any>, schema: Schema):Set<TypedInstance> {
      return facts.map { (typeName,attributes) ->
         TypedInstance.from(schema.type(typeName), attributes, schema)
      }.toSet()
   }
}
