package io.vyne.schemaStore

import io.vyne.schemas.CompositeSchema
import io.vyne.schemas.taxi.NamedSource
import io.vyne.schemas.taxi.TaxiSchema
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import java.io.Serializable

typealias SchemaSetId = Int

data class SchemaSet(val sources: List<VersionedSchema>, val generation: Int) {
   val id: Int = sources.hashCode()

   val taxiSchemas: List<TaxiSchema> by lazy {
      val namedSources = this.sources.map { it.namedSource }
      TaxiSchema.from(namedSources)
   }

   val rawSchemaStrings : List<String> = sources.map { it.content }

   val schema: CompositeSchema by lazy { CompositeSchema(this.taxiSchemas) }


   companion object {
      val EMPTY = SchemaSet(emptyList(), -1)
   }

   fun size() = sources.size

   fun add(schema: VersionedSchema): SchemaSet {
      return SchemaSet(this.sources + schema, this.generation + 1)
   }
}

typealias SchemaId = String

data class VersionedSchema(val name: String, val version: String, val content: String) : Serializable {
   val id: SchemaId = "$name:$version"

   val namedSource = NamedSource(content, id)
}

@RequestMapping("/schemas/taxi")
@FeignClient(name = "\${vyne.schemaStore.name}")
interface SchemaService {

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = ["/{schemaId}/{version}"])
   fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): VersionedSchema

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = ["/{schemaId}/{version}"])
   fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String)

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   fun listSchemas(): SchemaSet

}
