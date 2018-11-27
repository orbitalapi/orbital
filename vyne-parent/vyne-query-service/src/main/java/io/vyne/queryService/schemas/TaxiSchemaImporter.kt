package io.vyne.queryService.schemas

import io.vyne.schemaStore.VersionedSchema
import org.springframework.stereotype.Component

@Component
class TaxiSchemaImporter : SchemaImporter {
   override fun preview(request: SchemaPreviewRequest): SchemaPreview {
      return SchemaPreview(
         request.spec,
         request.content,
         emptyList()
      )
   }

   override fun import(request: SchemaImportRequest): VersionedSchema {
      return VersionedSchema(request.spec.name, request.spec.version, request.content)
   }

   override val supportedFormats: List<String> = listOf("taxi")


}
