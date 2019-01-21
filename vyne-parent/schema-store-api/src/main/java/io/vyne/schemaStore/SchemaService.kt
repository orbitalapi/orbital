package io.vyne.schemaStore

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import java.io.Serializable

typealias SchemaSetId = Int
data class SchemaSet(val schemas: List<VersionedSchema>) {
   val id: Int = schemas.hashCode()

   companion object {
      val EMPTY = SchemaSet(emptyList())
   }

   fun size() = schemas.size

   fun add(schema: VersionedSchema): SchemaSet {
      return SchemaSet(this.schemas + schema)
   }
}

typealias SchemaId  = String
data class VersionedSchema(val name: String, val version: String, val content: String) : Serializable {
   val id:SchemaId = "$name:$version"
}

@RequestMapping("/schemas/taxi")
@FeignClient(name = "\${vyne.schemaStore.name}")
interface SchemaService {

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = "/{schemaId}/{version}")
   fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): VersionedSchema

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = "/{schemaId}/{version}")
   fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String)

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   fun listSchemas(): SchemaSet

}
