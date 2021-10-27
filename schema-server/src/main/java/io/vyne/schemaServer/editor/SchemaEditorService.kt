package io.vyne.schemaServer.editor

import io.vyne.VersionedSource
import lang.taxi.types.QualifiedName
import mu.KotlinLogging
import org.http4k.quoted
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
// Can't use ConditionalOnBean on a RestController.  We could refactor this to ConditionOnExpression, but that would
// break the config mechanism of HOCON we're using.
//@ConditionalOnBean(ApiEditorRepository::class)
class SchemaEditorService(private val repository: ApiEditorRepository) : SchemaEditorApi {



   @GetMapping("/api/repository/editable")
   fun getConfig(): EditorServiceEnabledResponse {
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
      return generateAnnotationExtension(name, annotations, FileContentType.Annotations)

   }

   override fun updateDataOwnerOnType(typeName: String, request: UpdateDataOwnerRequest): Mono<SchemaEditResponse> {
      val name = QualifiedName.from(typeName)
      val annotation = """@io.vyne.catalog.DataOwner( id = ${request.id.quoted()} , name = ${request.name.quoted()} )"""
      return generateAnnotationExtension(name, annotation, FileContentType.DataOwner)
   }

   private fun generateAnnotationExtension(
      typeName: QualifiedName,
      annotationSource: String,
      contentType: FileContentType
   ): Mono<SchemaEditResponse> {
      val namespaceDeclaration = if (typeName.namespace.isNotEmpty()) {
         "namespace ${typeName.namespace}"
      } else ""
      val annotationSpec = """
$namespaceDeclaration

// This code is generated, and will be automatically updated
$annotationSource
type extension ${typeName.typeName} {}
      """.trimIndent()
         .trim()

      val filename = typeName.toFilename(contentType = contentType)
      repository.fileRepository.writeSource(
         VersionedSource.unversioned(filename, annotationSpec)
      )

      // TODO : Actual feedback...
      return Mono.just(SchemaEditResponse(true, emptyList()))
   }

}
