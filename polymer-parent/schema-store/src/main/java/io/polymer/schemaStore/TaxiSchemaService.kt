package io.polymer.schemaStore

import com.github.zafarkhaja.semver.Version
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.Compiler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidSchemaException(errors: List<CompilationError>) : RuntimeException(errors.map { it.detailMessage }.filterNotNull().joinToString())

@ResponseStatus(HttpStatus.BAD_REQUEST)
class SchemaExistsException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class UnknownResourceException(message: String) : RuntimeException(message)

data class VersionedSchema(val name: String, val version: Version, val content: String)
@RestController
@RequestMapping("/schemas/taxi")
class TaxiSchemaService {

   // TODO : Persist these somewhere
   private val schemas = mutableMapOf<String, VersionedSchema>()

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = "/{schemaId}/{version:.+}")
   fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): VersionedSchema {
      assertSchemaCompiles(schema)
      val semver = Version.valueOf(version)
      val versionedSchema = VersionedSchema(schemaId, semver, schema)
      addSchema(versionedSchema)
      return versionedSchema
   }

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = "/{schemaId}/{version:.+}")
   fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String) {
      val schema = schemas[schemaId] ?: throw UnknownResourceException("Schmea $schemaId was not found")
      val semver = Version.valueOf(version)
      if (schema.version !== semver) {
         throw UnknownResourceException("$schemaId does not exist at version $version.  We have version ${schema.version} instead")
      }
      schemas.remove(schemaId)
   }

   private fun addSchema(versionedSchema: VersionedSchema) {
      schemas[versionedSchema.name]?.let { existingSchema ->
         if (versionedSchema.version == existingSchema.version) {
            throw SchemaExistsException("Schema ${versionedSchema.name} with version ${versionedSchema.version} already exists with the same version.  Delete this resource if you wish to replace it")
         }
         if (versionedSchema.version < existingSchema.version) {
            throw SchemaExistsException("Schema ${versionedSchema.name} already exists with a later version (${existingSchema.version}).  Delete this resource if you wish to replace it")
         }
      }
      schemas.put(versionedSchema.name, versionedSchema)
   }

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   fun listSchemas(): List<VersionedSchema> {
      return schemas.values.toList()
   }

   private fun assertSchemaCompiles(schema: String) {
      try {
         Compiler(schema).compile()
      } catch (e: CompilationException) {
         throw InvalidSchemaException(e.errors)
      }
   }
}
