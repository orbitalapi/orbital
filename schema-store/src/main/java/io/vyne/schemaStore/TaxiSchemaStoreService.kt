package io.vyne.schemaStore

import arrow.core.getOrHandle
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationError
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidSchemaException(message:String) : RuntimeException(message) {
   constructor(errors: List<CompilationError>) : this(errors.map { it.detailMessage }.filterNotNull().joinToString())
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class SchemaExistsException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class UnknownResourceException(message: String) : RuntimeException(message)

@RestController
@RequestMapping("/api/schemas/taxi")
class TaxiSchemaStoreService(private val validatingStore: LocalValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient()) : SchemaStoreService, SchemaProvider {

   @RequestMapping(method = arrayOf(RequestMethod.POST))
   override fun submitSources(sources: List<VersionedSource>): SourceSubmissionResponse {
      return validatingStore.submitSchemas(sources)
         .map { SourceSubmissionResponse(emptyList(), validatingStore.schemaSet()) }
         .getOrHandle { exception -> SourceSubmissionResponse(exception.errors, validatingStore.schemaSet()) }
   }

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = ["/{schemaId}/{version:.+}"])
   override fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): SourceSubmissionResponse {
      return submitSources(listOf(VersionedSource(schemaId, version, schema)))
   }

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = ["/{schemaId}/{version:.+}"])
   override fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String) {
      TODO("Removing schema ${schemaId} not implemented")
   }

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   override fun listSchemas(): SchemaSet {
      return validatingStore.schemaSet()
   }

   @RequestMapping(path = arrayOf("/raw"), method = arrayOf(RequestMethod.GET))
   fun listRawSchema():String {
      return validatingStore.schemaSet().rawSchemaStrings.joinToString("\n")
   }

   override fun schemas(): List<Schema> {
      return validatingStore.schemaSet().taxiSchemas
   }
}
