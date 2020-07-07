package io.vyne.schemaStore

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.schemas.CompositeSchema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import lang.taxi.CompilationError
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*
import java.io.Serializable

typealias SchemaSetId = Int


@RequestMapping("/schemas/taxi")
@FeignClient(name = "\${vyne.schemaStore.name}")
interface SchemaService {

   @PostMapping
   fun submitSources(@RequestBody source: List<VersionedSource>): SourceSubmissionResponse


   @RequestMapping(method = arrayOf(RequestMethod.POST), value = ["/{schemaId}/{version}"])
   fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): VersionedSource

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = ["/{schemaId}/{version}"])
   fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String)

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   fun listSchemas(): SchemaSet
}

data class SourceSubmissionResponse(
   val parsedInputs: List<ParsedSource>,
   val schemaSet: SchemaSet
) {
   val isValid: Boolean = parsedInputs.all { it.isValid }
   val errors:List<CompilationError> = parsedInputs.flatMap { it.errors }

}
