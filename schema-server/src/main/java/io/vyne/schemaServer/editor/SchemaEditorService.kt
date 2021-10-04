package io.vyne.schemaServer.editor

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@RestController
@ConditionalOnProperty("vyne.schema-server.file.apiEditorProjectPath")
//@ConditionalOnBean(ApiEditorRepository::class)
class SchemaEditorService(private val repository: ApiEditorRepository) : SchemaEditorApi {
   init {
      logger.info { "SchemaEditorService is active, and writing changes to ${repository.fileRepository.projectPath}" }
   }

   @PostMapping("/api/repository/editable/sources")
   override fun submitEdits(
      @RequestBody request: SchemaEditRequest
   ): Mono<SchemaEditResponse> {
      logger.info {
         "Received request to edit the following sources: ${
            request.edits.joinToString("\n") { it.name }
         }"
      }
      repository.fileRepository.writeSources(request.edits)
      // TODO : Actual feedback...
      return Mono.just(SchemaEditResponse(true, emptyList()))
   }

   override fun updateAnnotationsOnType(
      typeName: String,
      request: UpdateTypeAnnotationRequest
   ): Mono<SchemaEditResponse> {
      TODO("Not yet implemented")
   }

}
