package io.vyne.schemaStore

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.zafarkhaja.semver.Version
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.Compiler
import lang.taxi.utils.log
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.atomic.AtomicInteger


@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidSchemaException(message:String) : RuntimeException(message) {
   constructor(errors: List<CompilationError>) : this(errors.map { it.detailMessage }.filterNotNull().joinToString())
}


@ResponseStatus(HttpStatus.BAD_REQUEST)
class SchemaExistsException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class UnknownResourceException(message: String) : RuntimeException(message)

@RestController
@RequestMapping("/schemas/taxi")
class TaxiSchemaService(
   private val schemaStore: SchemaStore
) : SchemaService, SchemaProvider {

   // TODO : Persist these somewhere
   private val schemas = mutableMapOf<String, VersionedSource>()
   private val generation:AtomicInteger = AtomicInteger(0);

   @PostMapping
   override fun submitSources(@RequestBody source:List<VersionedSource>) {

   }

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = ["/{schemaId}/{version:.+}"])
   override fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): VersionedSource {
      assertSchemaCompiles(schema)
      val versionedSchema = VersionedSource(schemaId, version, schema)
      addSchema(versionedSchema)
      log().info("Registered schema $schemaId:$version.  This schema server is now updated to schema set id ${listSchemas().id}")
      return versionedSchema
   }

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = ["/{schemaId}/{version:.+}"])
   override fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String) {
      val schema = schemas[schemaId] ?: throw UnknownResourceException("Schmea $schemaId was not found")
//      val semver = Version.valueOf(version)
//      if (schema.version !== semver) {
//         throw UnknownResourceException("$schemaId does not exist at version $version.  We have version ${schema.version} instead")
//      }
      schemas.remove(schemaId)
      generation.incrementAndGet()
   }

   private fun addSchema(versionedSchema: VersionedSource) {
      schemas[versionedSchema.name]?.let { existingSchema ->
         // TODO : Version checking
         // Have discabled for now, because YAGNI.  Bring it back
         // when there's a thought out requirement.
//         if (versionedSchema.version == existingSchema.version) {
//            throw SchemaExistsException("Schema ${versionedSchema.name} with version ${versionedSchema.version} already exists with the same version.  Delete this resource if you wish to replace it")
//         }
//         if (versionedSchema.version < existingSchema.version) {
//            throw SchemaExistsException("Schema ${versionedSchema.name} already exists with a later version (${existingSchema.version}).  Delete this resource if you wish to replace it")
//         }
      }
      schemas.put(versionedSchema.name, versionedSchema)
      generation.incrementAndGet()
   }

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   override fun listSchemas(): SchemaSet {
      return SchemaSet.from(schemas.values.toList(), generation.get())
   }
   @RequestMapping(path = arrayOf("/raw"), method = arrayOf(RequestMethod.GET))
   fun listRawSchema():String {
      return schemas.values.joinToString("\n") { it.content }
   }

   override fun schemas(): List<Schema> {
      return schemas.values
         .map { TaxiSchema.from(it.content) }
   }


   private fun assertSchemaCompiles(schema: String) {
      try {
         Compiler(schema).compile()
      } catch (e: CompilationException) {
         throw InvalidSchemaException(e.errors)
      } catch (e: Exception) {
         throw InvalidSchemaException("Unknown exception: ${e.message}")
      }
   }
}
