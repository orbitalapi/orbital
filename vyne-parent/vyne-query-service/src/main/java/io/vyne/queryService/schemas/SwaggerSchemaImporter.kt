package io.vyne.queryService.schemas

import io.vyne.schemaStore.VersionedSchema
import lang.taxi.generators.openApi.TaxiGenerator
import org.springframework.stereotype.Component

@Component
class SwaggerSchemaImporter : SchemaImporter {
   override val supportedFormats = listOf("swagger")
   private val generator = TaxiGenerator()
   override fun import(name: String, version: String, content: String): VersionedSchema {
      val taxi = generator.generateAsStrings(content,"foo").joinToString("\n")
      return VersionedSchema(name,version,taxi)
   }

}
