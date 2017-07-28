package io.polymer.schemaStore

import com.github.zafarkhaja.semver.Version
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

data class SchemaSet(val schemas: List<VersionedSchema>) {
   val id: Int = schemas.hashCode()
   companion object {
       val EMPTY = SchemaSet(emptyList())
   }

   fun size() = schemas.size
}

data class VersionedSchema(val name: String, val version: Version, val content: String)

@RequestMapping("/schemas/taxi")
interface SchemaService {

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = "/{schemaId}/{version:.+}")
   fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): VersionedSchema

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = "/{schemaId}/{version:.+}")
   fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String)

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   fun listSchemas(): SchemaSet

}
