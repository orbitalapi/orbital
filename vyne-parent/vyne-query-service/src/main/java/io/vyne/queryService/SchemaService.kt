package io.vyne.queryService

import io.vyne.queryService.schemas.SchemaImportRequest
import io.vyne.queryService.schemas.SchemaImportService
import io.vyne.queryService.schemas.SchemaPreview
import io.vyne.queryService.schemas.SchemaPreviewRequest
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.VersionedSchema
import io.vyne.schemaStore.VersionedSchemaProvider
import io.vyne.schemas.Schema
import lang.taxi.generators.SourceFormatter
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
class SchemaService(private val schemaProvider: SchemaSourceProvider, private val importer: SchemaImportService) {
   @GetMapping(path = ["/schemas/raw"])
   fun listRawSchema(): String {
      return schemaProvider.schemaStrings().joinToString("\n")
   }

   @GetMapping(path = ["/schemas"])
   fun getVersionedSchemas(): List<VersionedSchema> {
      return if (schemaProvider is VersionedSchemaProvider) {
         schemaProvider.versionedSchemas.sortedBy { it.name }
      } else {
         emptyList()
      }
   }

   @GetMapping(path = ["/types"])
   fun getTypes(): Schema {
      return schemaProvider.schema()
   }

   /**
    * Returns a schema comprised of types, and the types they reference.
    * Optionally, also includes Taxi primitives
    */
   @GetMapping(path = ["/schema"], params = ["members"])
   fun getTypes(
      @RequestParam("members") memberNames: List<String>,
      @RequestParam("includePrimitives", required = false) includePrimitives: Boolean = false): Schema {

      val result = schemaProvider.schema(memberNames, includePrimitives)
      return result;
   }

   @GetMapping(path = ["/schema"], params = ["members", "includeTaxi"])
   fun getTaxi(
      @RequestParam("members") memberNames: List<String>,
      @RequestParam("includePrimitives", required = false) includePrimitives: Boolean = false): SchemaWithTaxi {

      val schema = getTypes(memberNames, includePrimitives)

      val formatter = SourceFormatter(inlineTypeAliases = true)

      val typeSource = formatter.format(schema.types.map { it.sources.joinToString("\n") { it.content } }.joinToString("\n"))
      val operationSource = formatter.format(schema.services.map { it.sourceCode.joinToString("\n") { it.content } }.joinToString("\n"))

      val taxi = typeSource + "\n\n" + operationSource

      return SchemaWithTaxi(schema, taxi)
   }

   // TODO : MP - I don't think this is used
//   @PostMapping(path = ["/schemas"])
//   fun getTypesFromSchema(@RequestBody source: String): Schema {
//      return TaxiSchema.from(source)
//   }

   @PostMapping(path = ["/schemas"])
   fun submitSchema(@RequestBody request: SchemaImportRequest): Mono<VersionedSchema> {
      return importer.import(request)
   }

   @PostMapping(path = ["/schemas/preview"])
   fun previewSchema(@RequestBody request: SchemaPreviewRequest): Mono<SchemaPreview> {
      return importer.preview(request)
   }

}

data class SchemaWithTaxi(val schema: Schema, val taxi: String)
