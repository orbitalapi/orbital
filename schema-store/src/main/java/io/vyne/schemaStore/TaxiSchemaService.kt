package io.vyne.schemaStore

import arrow.core.extensions.either.applicativeError.handleError
import arrow.core.getOrHandle
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationError
import lang.taxi.utils.log
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidSchemaException(message: String) : RuntimeException(message) {
   constructor(errors: List<CompilationError>) : this(errors.map { it.detailMessage }.filterNotNull().joinToString())
}


@ResponseStatus(HttpStatus.BAD_REQUEST)
class SchemaExistsException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class UnknownResourceException(message: String) : RuntimeException(message)

@RestController
@RequestMapping("/schemas/taxi")
class TaxiSchemaService(
   private val schemaStoreClient: SchemaStoreClient
) : SchemaService, SchemaProvider {

   @PostMapping
   override fun submitSources(@RequestBody sources: List<VersionedSource>): SourceSubmissionResponse {
      return this.schemaStoreClient.submitSchemas(sources)
         .map { SourceSubmissionResponse(emptyList(), schemaStoreClient.schemaSet()) }
         .getOrHandle { exception -> SourceSubmissionResponse(exception.errors, schemaStoreClient.schemaSet()) }
   }

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = ["/{schemaId}/{version:.+}"])
   override fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): VersionedSource {
      val versionedSource = VersionedSource(schemaId, version, schema)
      schemaStoreClient.submitSchema(versionedSource)
         .handleError { error -> throw InvalidSchemaException(error.errors) }

      log().info("Registered schema ${versionedSource.id}.  This schema server is now updated to ${schemaStoreClient.schemaSet()}")
      return versionedSource
   }

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = ["/{schemaId}/{version:.+}"])
   override fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String) {
//      val schema = schemas[schemaId] ?: throw UnknownResourceException("Schmea $schemaId was not found")
////      val semver = Version.valueOf(version)
////      if (schema.version !== semver) {
////         throw UnknownResourceException("$schemaId does not exist at version $version.  We have version ${schema.version} instead")
////      }
//      schemas.remove(schemaId)
//      generation.incrementAndGet()
      // Is this still used?  I guess we need it for manually published schemas -
      // is there a different way?
      // Need to add support to schemaStoreClient.removeSchema(..)
      TODO()
   }

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   override fun listSchemas(): SchemaSet {
      return schemaStoreClient.schemaSet()
   }

   @RequestMapping(path = arrayOf("/raw"), method = arrayOf(RequestMethod.GET))
   fun listRawSchema(): List<VersionedSource> {
      return schemaStoreClient.schemaSet().allSources
   }

   override fun schemas(): List<Schema> {
      return schemaStoreClient.schemaSet().taxiSchemas
   }

}
