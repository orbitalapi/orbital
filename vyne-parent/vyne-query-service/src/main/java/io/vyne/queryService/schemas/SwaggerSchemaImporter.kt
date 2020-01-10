package io.vyne.queryService.schemas

import io.vyne.schemaStore.VersionedSchema
import lang.taxi.generators.openApi.TaxiGenerator
import org.springframework.stereotype.Component
import v2.io.swagger.parser.SwaggerParser

@Component
class SwaggerSchemaImporter : SchemaImporter {
   companion object {
      const val SWAGGER_FORMAT = "swagger"
   }

   override fun preview(request: SchemaPreviewRequest): SchemaPreview {
      val spec = getSpec(request.content)
      val generationResult = generator.generateAsStrings(request.content, spec.defaultNamespace!!)
      val taxi = generationResult.taxi.joinToString("\n")
      return SchemaPreview(
         spec,
         taxi,
         generationResult.messages
      )
   }

   private fun getSpec(content: String): DraftSchemaSpec {
      val swagger = SwaggerParser().parse(content)
      return DraftSchemaSpec(
         name = swagger.info?.title,
         version = swagger.info?.version,
         defaultNamespace = Namespaces.hostToNamespace(swagger.host)
      )
   }

   override val supportedFormats = listOf(SWAGGER_FORMAT)
   private val generator = TaxiGenerator()
   override fun import(request: SchemaImportRequest): VersionedSchema {

      val taxi = generator.generateAsStrings(request.content, request.spec.defaultNamespace).taxi.joinToString("\n")
      return VersionedSchema(request.spec.name, request.spec.version, taxi)
   }

}
