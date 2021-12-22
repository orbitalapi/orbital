package io.vyne.queryService.schemas.importing

import io.vyne.queryService.OperationNotPermittedException
import io.vyne.queryService.QueryServerConfig
import io.vyne.queryService.schemas.editor.SchemaSubmissionResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SchemaImporterService(
   private val importer: CompositeSchemaImporter,
   private val config: QueryServerConfig,
) {

   // TODO : What's the relationship between this and the schema-store-api?
   // Should probably either align the two api's or remove one.
   // Looks like schema-store-api isn't used anywhere.
   @PostMapping(path = ["/api/schemas/import"])
   fun submitSchema(@RequestBody request: SchemaConversionRequest, @RequestParam("dryRun", defaultValue = "false") dryRun:Boolean = false): Mono<SchemaSubmissionResult> {
      if (!config.newSchemaSubmissionEnabled) {
         throw OperationNotPermittedException()
      }
      if (dryRun) {
         return importer.preview(request)
      } else {
         return importer.import(request)
      }

   }


}
