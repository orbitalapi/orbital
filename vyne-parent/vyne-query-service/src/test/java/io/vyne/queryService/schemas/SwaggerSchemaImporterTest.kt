package io.vyne.queryService.schemas

import com.google.common.io.Resources
import com.winterbe.expekt.expect
import org.junit.Test

class SwaggerSchemaImporterTest {
   val importer = SwaggerSchemaImporter()

   @Test
   fun canImportSwaggerV3Schema() {
      val swagger = Resources.getResource("jira-swagger-v3.json").readText()
      val schemaPreview = importer.preview(SchemaPreviewRequest(
         DraftSchemaSpec("jira", "1", "com.jira"),
         SwaggerSchemaImporter.SWAGGER_FORMAT,
         swagger,
         null
      ))

      expect(schemaPreview.content).to.be.not.empty
   }

}

