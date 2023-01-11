package io.vyne.schemaServer.core.editor

import io.vyne.PackageIdentifier
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.publisher.loaders.*
import io.vyne.schema.publisher.loaders.AddChangesToChangesetResponse
import io.vyne.schema.publisher.loaders.AvailableChangesetsResponse
import io.vyne.schema.publisher.loaders.CreateChangesetResponse
import io.vyne.schema.publisher.loaders.FinalizeChangesetResponse
import io.vyne.schema.publisher.loaders.SetActiveChangesetResponse
import io.vyne.schema.publisher.loaders.UpdateChangesetResponse
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoader
import io.vyne.schemaServer.core.file.packages.FileSystemPackageWriter
import io.vyne.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import io.vyne.schemaServer.editor.*
import io.vyne.schemas.toVyneQualifiedName
import lang.taxi.types.QualifiedName
import mu.KotlinLogging
import org.http4k.quoted
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger {}

@RestController
// Can't use ConditionalOnBean on a RestController.  We could refactor this to ConditionOnExpression, but that would
// break the config mechanism of HOCON we're using.
//@ConditionalOnBean(ApiEditorRepository::class)
class SchemaEditorService(
   private val repositoryManager: ReactiveRepositoryManager, private val schemaProvider: SchemaStore
) : SchemaEditorApi {


   @GetMapping("/api/repository/editable")
   override fun getEditorConfig(): Mono<EditableRepositoryConfig> {

      val sourcePackages: List<Mono<SourcePackage>> = repositoryManager.editableLoaders.map { it.loadNow() }
      return Flux.concat(sourcePackages).collectList().map { packages ->
         val identifiers = packages.map { it.identifier }
         EditableRepositoryConfig(identifiers)
      }
   }

   @PostMapping("/api/repository/changeset/create")
   override fun createChangeset(
      @RequestBody request: StartChangesetRequest
   ): Mono<CreateChangesetResponse> {
      return Mono.just(request)
         .subscribeOn(Schedulers.boundedElastic())
         .flatMap {
            logger.info { "Received request to start a changeset with name ${request.changesetName}" }
            val loader = repositoryManager.getLoader(request.packageIdentifier)
            loader.createChangeset(request.changesetName)
         }
   }

   @PostMapping("/api/repository/changeset/add")
   override fun addChangesToChangeset(
      @RequestBody request: AddChangesToChangesetRequest
   ): Mono<AddChangesToChangesetResponse> {
      logger.info {
         "Received request to add changes to the changeset with name ${request.changesetName} for the following sources: ${
            request.edits.joinToString("\n") { it.name }
         }"
      }
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      return loader.addChangesToChangeset(request.changesetName, request.edits)
   }

   @PostMapping("/api/repository/changeset/finalize")
   override fun finalizeChangeset(
      @RequestBody request: FinalizeChangesetRequest
   ): Mono<FinalizeChangesetResponse> {
      logger.info { "Received request to finalize the changeset with name ${request.changesetName}" }
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      return loader.finalizeChangeset(request.changesetName)
   }


   @PutMapping("/api/repository/changeset/update")
   override fun updateChangeset(
      @RequestBody request: UpdateChangesetRequest
   ): Mono<UpdateChangesetResponse> {
      logger.info { "Received request to update the changeset with name ${request.changesetName}" }
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      return loader.updateChangeset(request.changesetName, request.newChangesetName)
   }

   @PostMapping("/api/repository/changesets")
   override fun getAvailableChangesets(
      @RequestBody request: GetAvailableChangesetsRequest
   ): Mono<AvailableChangesetsResponse> {
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      return loader.getAvailableChangesets()
   }

   @PostMapping("/api/repository/changesets/active")
   override fun setActiveChangeset(
      @RequestBody request: SetActiveChangesetRequest
   ): Mono<SetActiveChangesetResponse> {
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      return loader.setActiveChangeset(request.changesetName)
   }

   // TODO What to do about this method
   @PostMapping("/api/repository/editable/sources")
   override fun submitEdits(
      @RequestBody request: SchemaEditRequest
   ): Mono<SchemaEditResponse> {
      logger.info {
         "Received request to edit the following sources: ${
            request.edits.joinToString("\n") { it.name }
         }"
      }
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      val writer = FileSystemPackageWriter()
      return writer.writeSources(loader as FileSystemPackageLoader, request.edits).map {
         // TODO : Actual feedback...
         SchemaEditResponse(true, emptyList())
      }
   }

   override fun updateAnnotationsOnType(
      typeName: String, request: UpdateTypeAnnotationRequest
   ): Mono<AddChangesToChangesetResponse> {
      // This is a very naieve demo-ready implementation.
      // It doesn't actually work, as it ignores other locations

      val name = QualifiedName.from(typeName)
      val annotations = request.annotations.joinToString("\n") { it.asTaxi() }
      return generateAnnotationExtension(request.changeset, name, annotations, FileContentType.Annotations)
   }

   override fun updateDataOwnerOnType(
      typeName: String, request: UpdateDataOwnerRequest
   ): Mono<AddChangesToChangesetResponse> {
      val name = QualifiedName.from(typeName)
      val annotation = """@io.vyne.catalog.DataOwner( id = ${request.id.quoted()} , name = ${request.name.quoted()} )"""
      return generateAnnotationExtension(request.changeset, name, annotation, FileContentType.DataOwner)
   }

   private fun generateAnnotationExtension(
      changeset: Changeset, typeName: QualifiedName, annotationSource: String, contentType: FileContentType
   ): Mono<AddChangesToChangesetResponse> {
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
      """.trimIndent().trim()

      val filename = typeName.toFilename(contentType = contentType)
      return addChangesToChangeset(
         AddChangesToChangesetRequest(
            changeset.name,
            changeset.packageIdentifier,
            listOf(VersionedSource.unversioned(filename, annotationSpec))
         )
      )
   }

   // Short term workaround...
   // For now, only support editing of a single package.
   // However, we'll need to allow multiple editable packages,
   // and edit requests will have to tell us which package the
   // edit should go to.
   private fun getDefaultEditablePackage(): Mono<PackageIdentifier> {
      return this.getEditorConfig().map { config ->
         config.getDefaultEditorPackage()
      }
   }
}
