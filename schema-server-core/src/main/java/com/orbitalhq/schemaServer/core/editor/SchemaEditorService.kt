package com.orbitalhq.schemaServer.core.editor

import com.google.common.collect.Sets
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.VyneTypes
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schema.publisher.loaders.*
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoader
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageWriter
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader
import com.orbitalhq.schemaServer.core.git.GitWriterDecorator
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaServer.editor.*
import com.orbitalhq.schemas.SavedQuery
import com.orbitalhq.schemas.taxi.asSavedQuery
import com.orbitalhq.schemas.taxi.toMessage
import com.orbitalhq.schemas.toVyneQualifiedName
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.BadRequestException
import lang.taxi.errors
import lang.taxi.types.QualifiedName
import mu.KotlinLogging
import org.http4k.quoted
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger {}

@RestController
// Can't use ConditionalOnBean on a RestController.  We could refactor this to ConditionOnExpression, but that would
// break the config mechanism of HOCON we're using.
//@ConditionalOnBean(ApiEditorRepository::class)
class SchemaEditorService(
    private val repositoryManager: ReactiveProjectStoreManager, private val schemaProvider: SchemaStore
)  {


   @GetMapping("/api/repository/editable")
   fun getEditorConfig(): Mono<EditableRepositoryConfig> {

      val sourcePackages: List<Mono<SourcePackage>> = repositoryManager.editableLoaders.map { it.loadNow() }
      return Flux.concat(sourcePackages).collectList().map { packages ->
         val identifiers = packages.map { it.identifier }
         EditableRepositoryConfig(identifiers)
      }
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/repository/changeset/create")
   fun createChangeset(
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

   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/repository/queries")
   fun saveQuery(@RequestBody request: SaveQueryRequest): Mono<SavedQuery> {
      return Mono.just(request)
         .subscribeOn(Schedulers.boundedElastic())
         .map { request ->

            // If the inbound query doesn't yet have a name, we give it one.
            val queryWithName = QueryEditor.prependQueryBlockIfMissing(request.source)

            // First validate that the query is, well...y'know, valid.
            val currentQueries = schemaProvider.schema().taxi.queries
            val (messages, taxiDoc) = SchemaEditValidator.validate(
               listOf(queryWithName),
               schemaProvider.schema().asTaxiSchema()
            )
            if (messages.errors().isNotEmpty()) {
               throw BadRequestException("The query has compilation errors, so cannot be saved: ${messages.errors().toMessage()}")
            }
            val queryDifferences = Sets.difference(taxiDoc.queries, currentQueries)
            require(queryDifferences.size == 1) { "Expected a single new query, but found ${queryDifferences.size}" }
            queryDifferences.single() to request
         }
         .flatMap { (taxiQuery, _) ->
            val queryWithName = QueryEditor.prependQueryBlockIfMissing(request.source)
            addChangesToChangeset(
               AddChangesToChangesetRequest(
                  request.changesetName,
                  request.source.packageIdentifier!!,
                  listOf(queryWithName)
               )
            ).map {
               taxiQuery.asSavedQuery(request.source.packageIdentifier)
            }
         }
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/repository/changeset/add")
   fun addChangesToChangeset(
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

   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/repository/changeset/finalize")
   fun finalizeChangeset(
      @RequestBody request: FinalizeChangesetRequest
   ): Mono<FinalizeChangesetResponse> {
      logger.info { "Received request to finalize the changeset with name ${request.changesetName}" }
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      return loader.finalizeChangeset(request.changesetName)
   }


   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PutMapping("/api/repository/changeset/update")
   fun updateChangeset(
      @RequestBody request: UpdateChangesetRequest
   ): Mono<UpdateChangesetResponse> {
      logger.info { "Received request to update the changeset with name ${request.changesetName}" }
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      return loader.updateChangeset(request.changesetName, request.newChangesetName)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/repository/changesets")
   fun getAvailableChangesets(
      @RequestBody request: GetAvailableChangesetsRequest
   ): Mono<AvailableChangesetsResponse> {
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      return loader.getAvailableChangesets()
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/repository/changesets/active")
   fun setActiveChangeset(
      @RequestBody request: SetActiveChangesetRequest
   ): Mono<SetActiveChangesetResponse> {
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      return loader.setActiveChangeset(request.changesetName)
   }

   // TODO What to do about this method
   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/repository/editable/sources")
   fun submitEdits(
      @RequestBody request: SchemaEditRequest
   ): Mono<SchemaEditResponse> {
      logger.info {
         "Received request to edit the following sources: ${
            request.edits.joinToString("\n") { it.name }
         }"
      }
      val loader = repositoryManager.getLoader(request.packageIdentifier)
      val (fileSystemLoader, writerMessages) = when(loader) {
         is FileSystemPackageLoader -> loader to emptyList()

         // If we're writing to a git backed store, we need to
         // tell the user that they're responsible for the git push.
         is GitSchemaPackageLoader -> loader.filePackageLoader to listOf(
            GitWriterDecorator.GIT_COMMIT_NEEDED
         )
         else -> error("Unexpected type of SchemaPackageTransport: ${loader::class.simpleName}")
      }
      val writer = FileSystemPackageWriter()
      return writer.writeSources(fileSystemLoader, request.edits).map {
         // TODO : Actual feedback...
         SchemaEditResponse(true, emptyList(), writerMessages)
      }
   }

   fun updateAnnotationsOnType(
      typeName: String, request: UpdateTypeAnnotationRequest
   ): Mono<AddChangesToChangesetResponse> {
      // This is a very naieve demo-ready implementation.
      // It doesn't actually work, as it ignores other locations

      val name = QualifiedName.from(typeName)
      val annotations = request.annotations.joinToString("\n") { it.asTaxi() }
      return generateAnnotationExtension(request.changeset, name, annotations, FileContentType.Annotations)
   }

   fun updateDataOwnerOnType(
      typeName: String, request: UpdateDataOwnerRequest
   ): Mono<AddChangesToChangesetResponse> {
      val name = QualifiedName.from(typeName)
      val annotation = """@${VyneTypes.NAMESPACE}.catalog.DataOwner( id = ${request.id.quoted()} , name = ${request.name.quoted()} )"""
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
