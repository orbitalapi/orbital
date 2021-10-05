package io.vyne.schemaServer.editor

import io.vyne.VersionedSource
import lang.taxi.types.QualifiedName
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

/**
 * Simple class to return to indicate that the editor service is active
 */
data class EditorServiceEnabledResponse(
   val repositoryId: String
) {
   val enabled = true
}
@RestController
//@ConditionalOnProperty("vyne.schema-server.file.apiEditorProjectPath")
@ConditionalOnBean(ApiEditorRepository::class)
class SchemaEditorService(private val repository: ApiEditorRepository) : SchemaEditorApi {
   init {
      logger.info { "SchemaEditorService is active, and writing changes to ${repository.fileRepository.projectPath}" }
   }

   @GetMapping("/api/repository/editable")
   fun getConfig():EditorServiceEnabledResponse {
      return EditorServiceEnabledResponse(repository.fileRepository.identifier)
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
      // This is a very naieve demo-ready implementation.
      // It doesn't actually work, as it ignores other locations

      val name = QualifiedName.from(typeName)
      val annotations = request.annotations.joinToString("\n") { "@" + it.fullyQualifiedName }
      val namespaceDeclaration = if (name.namespace.isNotEmpty()) {
         "namespace ${name.namespace}"
      } else ""
      val annotationSpec = """
$namespaceDeclaration

// This code is generated, and will be automatically updated
$annotations
type extension ${name.typeName} {}
      """.trimIndent()
         .trim()

      val filename = name.toFilename(contentType = FileContentType.Annotations)
      repository.fileRepository.writeSource(
         VersionedSource.unversioned(filename, annotationSpec)
      )

      // TODO : Actual feedback...
      return Mono.just(SchemaEditResponse(true, emptyList()))

   }

}
