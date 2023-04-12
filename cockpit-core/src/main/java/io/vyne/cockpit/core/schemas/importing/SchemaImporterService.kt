package io.vyne.cockpit.core.schemas.importing

import io.vyne.cockpit.core.schemas.editor.SchemaSubmissionResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SchemaImporterService(
   private val importer: CompositeSchemaImporter,
) {

   // TODO : What's the relationship between this and the schema-store-api?
   // Should probably either align the two api's or remove one.
   // Looks like schema-store-api isn't used anywhere.
   @PostMapping(path = ["/api/schemas/import"])
   fun submitSchema(
      @RequestBody request: SchemaConversionRequest,
      @RequestParam("validateOnly", defaultValue = "false") validateOnly: Boolean = false
   ): Mono<SchemaSubmissionResult> {
      return if (validateOnly) {
         importer.preview(request)
      } else {
         importer.import(request)
      }

   }


}
