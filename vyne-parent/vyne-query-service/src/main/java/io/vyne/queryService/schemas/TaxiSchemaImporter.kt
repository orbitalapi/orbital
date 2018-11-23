package io.vyne.queryService.schemas

import io.vyne.schemaStore.VersionedSchema
import org.springframework.stereotype.Component

@Component
class TaxiSchemaImporter : SchemaImporter {
   override val supportedFormats: List<String> = listOf("taxi")

   override fun import(name: String, version: String, content: String): VersionedSchema {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

}
