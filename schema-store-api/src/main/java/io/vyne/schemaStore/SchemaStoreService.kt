package io.vyne.schemaStore

import io.vyne.VersionedSource
import lang.taxi.CompilationError
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@RequestMapping("/api/schemas/taxi")
@ReactiveFeignClient("\${vyne.queryService.name:query-service}")
interface SchemaStoreService {

   @RequestMapping(method = arrayOf(RequestMethod.POST))
   fun submitSources(@RequestBody schemas: List<VersionedSource>): Mono<SourceSubmissionResponse>

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = ["/{schemaId}/{version}"])
   fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): Mono<SourceSubmissionResponse>

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = ["/{schemaId}/{version}"])
   fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): Mono<String>

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   fun listSchemas(): Mono<SchemaSet>

}
data class SourceSubmissionResponse(
   val errors: List<CompilationError>,
   val schemaSet: SchemaSet){
   val isValid: Boolean = errors.isEmpty()
}

