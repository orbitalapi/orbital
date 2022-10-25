package io.vyne.schemaServer.core.editor

import io.vyne.PackageIdentifier
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemaServer.core.file.packages.FileSystemPackageWriter
import io.vyne.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import io.vyne.schemaServer.editor.*
import io.vyne.schemas.toVyneQualifiedName
import lang.taxi.types.QualifiedName
import mu.KotlinLogging
import org.http4k.quoted
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@RestController
// Can't use ConditionalOnBean on a RestController.  We could refactor this to ConditionOnExpression, but that would
// break the config mechanism of HOCON we're using.
//@ConditionalOnBean(ApiEditorRepository::class)
class SchemaEditorService(
   private val repositoryManager: ReactiveRepositoryManager,
   private val schemaProvider: SchemaStore
) :
   SchemaEditorApi {


   @GetMapping("/api/repository/editable")
   override fun getEditorConfig(): Mono<EditableRepositoryConfig> {

      val sourcePackages: List<Mono<SourcePackage>> = repositoryManager.editableLoaders
         .map { it.loadNow() }
      return Flux.concat(sourcePackages)
         .collectList()
         .map { packages ->
            val identifiers = packages.map { it.identifier }
            EditableRepositoryConfig(identifiers)
         }
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
      // repository.fileRepository.writeSources(request.edits)
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      val writer = FileSystemPackageWriter()
      return writer.writeSources(loader, request.edits)
         .map {
            // TODO : Actual feedback...
            SchemaEditResponse(true, emptyList())
         }
   }

   override fun updateAnnotationsOnType(
      typeName: String,
      request: UpdateTypeAnnotationRequest
   ): Mono<SchemaEditResponse> {
      // This is a very naieve demo-ready implementation.
      // It doesn't actually work, as it ignores other locations

      val name = QualifiedName.from(typeName)
      val annotations = request.annotations.joinToString("\n") { it.asTaxi() }
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
      val type = schemaProvider.schemaSet.schema.type(typeName.toVyneQualifiedName())
      val tokenType = when {
         type.isEnum -> "enum"
         else -> "type"
      }

      val namespaceDeclaration = if (typeName.namespace.isNotEmpty()) {
         "namespace ${typeName.namespace}"
      } else ""
      val annotationSpec = """
$namespaceDeclaration

// This code is generated, and will be automatically updated
$annotationSource
$tokenType extension ${typeName.typeName} {}
      """.trimIndent()
         .trim()

      val filename = typeName.toFilename(contentType = contentType)
      return getDefaultEditablePackage().flatMap { packageIdentifier ->
         submitEdits(
            SchemaEditRequest(
               packageIdentifier,
               listOf(VersionedSource.unversioned(filename, annotationSpec))
            )
         )
      }
   }

   // Short term workaround...
   // FOr now, only support editing of a single package.
   // However, we'll need to allow mutliple editable pacakages,
   // and edit requests will have to tell us which package the
   // edit should go to.
   private fun getDefaultEditablePackage(): Mono<PackageIdentifier> {
      return this.getEditorConfig()
         .map { config ->
            config.getDefaultEditorPackage()
         }
   }
}
