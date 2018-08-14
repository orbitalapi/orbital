package io.vyne.queryService

import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import io.polymer.schemaStore.SchemaSourceProvider
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

   @PostMapping(path = ["/schemas"])
   fun getTypesFromSchema(@RequestBody source: String): Schema {
      return TaxiSchema.from(source)
   }
}
