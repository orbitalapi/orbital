package io.vyne.queryService

import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.generators.SourceFormatter
import org.springframework.web.bind.annotation.*

@RestController
class SchemaService(private val schemaProvider: SchemaSourceProvider) {
   @GetMapping(path = ["/schemas/raw"])
   fun listRawSchema(): String {
      return schemaProvider.schemaStrings().joinToString("\n")
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

   @PostMapping(path = ["/schemas"])
   fun getTypesFromSchema(@RequestBody source: String): Schema {
      return TaxiSchema.from(source)
   }
}

data class SchemaWithTaxi(val schema: Schema, val taxi: String)
