package io.vyne.schemaStore

import io.vyne.VersionedSource
import lang.taxi.CompilationError
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@RequestMapping("/api/schemas/taxi")
@FeignClient(name = "query-service")
interface SchemaStoreService {

   @RequestMapping(method = arrayOf(RequestMethod.POST))
   fun submitSources(@RequestBody schemas: List<VersionedSource>): SourceSubmissionResponse

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = ["/{schemaId}/{version}"])
   fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): SourceSubmissionResponse

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = ["/{schemaId}/{version}"])
   fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String)

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   fun listSchemas(): SchemaSet

}
data class SourceSubmissionResponse(
   val errors: List<CompilationError>,
   val schemaSet: SchemaSet){
   val isValid: Boolean = errors.isEmpty()
}

