package io.vyne.cockpit.core.schemas.editor


import arrow.core.*
import com.google.common.collect.Sets
import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.cockpit.core.schemas.editor.generator.VyneSchemaToTaxiGenerator
import io.vyne.cockpit.core.schemas.editor.operations.SchemaEdit
import io.vyne.cockpit.core.schemas.editor.splitter.SingleTypePerFileSplitter
import io.vyne.cockpit.core.schemas.editor.splitter.SourceSplitter
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemaServer.editor.SchemaEditRequest
import io.vyne.schemaServer.editor.SchemaEditResponse
import io.vyne.schemaServer.editor.SchemaEditValidator
import io.vyne.schemaServer.editor.SchemaEditorApi
import io.vyne.schemaServer.packages.PackagesServiceApi
import io.vyne.schemas.*
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.schemas.taxi.filtered
import io.vyne.schemas.taxi.toVyneQualifiedName
import io.vyne.spring.http.BadRequestException
import io.vyne.spring.http.handleFeignErrors
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.errors
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Compiled
import lang.taxi.types.ImportableToken
import lang.taxi.types.Type
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

@RestController
class LocalSchemaEditingService(
   private val packagesServiceApi: PackagesServiceApi,
   private val schemaEditorApi: SchemaEditorApi,
   private val schemaStore: SchemaStore

) {
   fun getEditorConfig() = schemaEditorApi.getEditorConfig()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   /**
    * Submits an edit to the source of a schema.
    *
    * This approach is experimental, but intended to replace submitEditedSchema,
    * which relies on Vyne Schema -> Taxi generation, and gets complex with
    * schemas that are merged together.
    *
    * Instead, this approach is intended to provide small edits to source files.
    */
   @PostMapping("/api/schemas/edits")
   fun submitSchemaEditOperation(
      @RequestBody edit: SchemaEdit
   ): Mono<SchemaSubmissionResult> {
      val schema = schemaStore.schema()
      return packagesServiceApi.loadPackage(edit.packageIdentifier.uriSafeId)
         .flatMap { packageWithDescription ->
            val currentSourcePackage = packageWithDescription.parsedPackage.toSourcePackage()
            val initial: Either<CompilationException, Pair<SourcePackage, TaxiDocument>> =
               (currentSourcePackage to schema.asTaxiSchema().taxi).right()
            val editResult = edit.edits
               .fold(initial) { acc, editOperation ->
                  acc.flatMap { (currentSource, currentTaxi) ->
                     editOperation.applyTo(currentSource, currentTaxi)
                  }
               }

            val (updatedSourcePackage, updatedTaxi) = editResult.getOrElse { throw it }
            val (compilationMessages, updatedTaxiSchema) = TaxiSchema.compiled(
               listOf(updatedSourcePackage),
               imports = listOf(schema.asTaxiSchema())
            )

            val pendingUpdates = if (edit.dryRun) {
               edit.edits
            } else {
               emptyList()
            }
            val submissionResult = SchemaSubmissionResult(
               updatedTaxiSchema.types,
               updatedTaxiSchema.services,
               compilationMessages,
               "",
               edit.dryRun,
               updatedSourcePackage,
               pendingUpdates
            )

            if (edit.dryRun) {
               Mono.just(submissionResult)
            } else {
               submitEdits(updatedSourcePackage).map { submissionResult }
            }
         }
   }

   /**
    * Submits an actual schema (a subset of it - just types and services/operations).
    * The schema is used to generate taxi.
    * Note that any taxi present in the types & services is ignored.
    * This operation is used when importing / editing from the UI, and is an approach which
    * reduces / eliminates the need for client-side taxi generation code.
    */
   @Deprecated("use submitSchemaEditOperation instead")
   @PostMapping("/api/schemas/edit", consumes = [MediaType.APPLICATION_JSON_VALUE])
   fun submitEditedSchema(
      @RequestBody editedSchema: EditedSchema,
      @RequestParam("packageIdentifier") rawPackageIdentifier: String,
      @RequestParam("validate", required = false) validateOnly: Boolean = false,
   ): Mono<SchemaSubmissionResult> {
      logger.info {
         "Received request to edit schema: \n " +
            "types: ${editedSchema.types.map { it.fullyQualifiedName }} \n " +
            "services: ${editedSchema.services.map { it.name.fullyQualifiedName }}"
      }
      return ensureTargetPackageIsEditable(rawPackageIdentifier).flatMap {
         ensureSinglePackageForTypeOrService(PackageIdentifier.fromId(rawPackageIdentifier), editedSchema)
         val generator = VyneSchemaToTaxiGenerator()
         val existingPartialSchemaForEditedPackage =
            this.schemaStore.schemaSet.schema.getPartialSchemaForPackage(rawPackageIdentifier)
         //getPartialSchemaForPackage(rawPackageIdentifier)
         val generated = generator.generateWithPackageUpsertDelete(
            PackageIdentifier.fromId(rawPackageIdentifier),
            editedSchema,
            existingPartialSchemaForEditedPackage,
            this::getCurrentSchemaExcluding
         )
         if (generated.messages.isNotEmpty()) {
            val message =
               "Generation of taxi completed - ${generated.messages.size} messages: \n ${
                  generated.messages.joinToString(
                     "\n"
                  )
               }"
            if (generated.hasWarnings || generated.hasErrors) {
               logger.warn { message }
            } else {
               logger.info { message }
            }
         } else {
            logger.info { "Generation of taxi completed - no messages or warnings were produced" }
         }
         doSubmit(generated, validateOnly, rawPackageIdentifier)
      }
   }

   // If a type has definitions across multiple packages, we should reject.
   private fun ensureSinglePackageForTypeOrService(packageIdentifier: PackageIdentifier, editedSchema: EditedSchema) {
      val otherPackages =
         schemaStore.schemaSet.packages.filter { it.identifier != packageIdentifier }.map { it.identifier }
      val typesInOtherPackages = schemaStore
         .schemaSet
         .schema
         .types.filter { it.sources.any { source -> source.packageIdentifier != null && otherPackages.contains(source.packageIdentifier) } }
         .map { it.qualifiedName.fullyQualifiedName }
         .toSet()

      val servicesInOtherPackages = schemaStore
         .schemaSet
         .schema
         .services.filter {
            it.sourceCode.any { source ->
               source.packageIdentifier != null && otherPackages.contains(
                  source.packageIdentifier
               )
            }
         }
         .map { it.qualifiedName }
         .toSet()

      val dupTypes = Sets.intersection(typesInOtherPackages, editedSchema.types.map { it.fullyQualifiedName }.toSet())
      if (dupTypes.isNotEmpty()) {
         val errorMessage = dupTypes.map {
            val definitionsInExistingPackages =
               schemaStore.schemaSet.schema.type(it).sources.joinToString { sourceCode ->
                  sourceCode.packageIdentifier?.id ?: ""
               }
            "${QualifiedName.from(it).shortDisplayName} is defined in $definitionsInExistingPackages"
         }
         throw BadRequestException("Editing types with definitions in multiple packages is not supported. $errorMessage")
      }

      val dupServices =
         Sets.intersection(servicesInOtherPackages, editedSchema.services.map { it.name.fullyQualifiedName }.toSet())
      if (dupServices.isNotEmpty()) {
         val errorMessage = dupServices.map {
            val definitionsInExistingPackages =
               schemaStore.schemaSet.schema.service(it).sourceCode.joinToString { sourceCode ->
                  sourceCode.packageIdentifier?.id ?: ""
               }
            "${QualifiedName.from(it).shortDisplayName} is defined in $definitionsInExistingPackages"
         }
         throw BadRequestException("Editing services with definitions in multiple packages is not supported. $errorMessage")
      }
   }

   // Changes need to be part of an editable package
   private fun ensureTargetPackageIsEditable(rawPackageIdentifier: String): Mono<Unit> {
      val packageIdentifier = PackageIdentifier.fromId(rawPackageIdentifier)
      return packagesServiceApi
         .listPackages()
         .map { it.firstOrNull { f -> f.identifier == packageIdentifier } }
         .map { if (it?.editable == true) Unit else throw BadRequestException("$rawPackageIdentifier is not editable") }
   }

   /**
    * Returns a TaxiSchema that does not contain definitions
    * for the types or services provided
    */
   private fun getCurrentSchemaExcluding(types: Set<PartialType>, services: Set<PartialService>): TaxiSchema {
      val currentSchema = schemaStore.schemaSet.schema
      val typeNames: Set<QualifiedName> = types.map { it.name }.toSet()
      val serviceNames = services.map { it.name }.toSet()
      // Expect that there's only a single taxi schema.  We've migrated schema handling
      // so that the schemaStore just composes everything into a single taxi schema.
      val taxiSchema = currentSchema.taxiSchemas.single()
      val filteredTaxiDocument = taxiSchema.document.filtered(
         typeFilter = { type: Type -> !typeNames.contains(type.toVyneQualifiedName()) },
         serviceFilter = { service -> !serviceNames.contains(service.toQualifiedName().toVyneQualifiedName()) }
      )
      return TaxiSchema(
         filteredTaxiDocument, taxiSchema.packages, taxiSchema.functionRegistry
      )
   }

   /**
    * Submit a taxi string containing schema changes.
    * Updates are persisted to the local schema repository, and then published to
    * the schema store.
    *
    * The updated Vyne types containing in the Taxi string are returned.
    */
   @PostMapping(
      "/api/schema/taxi/{packageIdentifier}",
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE]
   )
   fun submit(
      @RequestBody taxi: String,
      @RequestParam("validate", required = false) validateOnly: Boolean = false,
      @PathVariable("packageIdentifier") rawPackageIdentifier: String
   ): Mono<SchemaSubmissionResult> {
      return doSubmit(GeneratedTaxiCode(listOf(taxi), emptyList()), validateOnly, rawPackageIdentifier)
   }

   fun submitEdits(sourcePackage: SourcePackage): Mono<SchemaEditResponse> {
      return submitEdits(sourcePackage.sources, sourcePackage.identifier)
   }

   fun submitEdits(
      versionedSources: List<VersionedSource>,
      packageIdentifier: PackageIdentifier
   ): Mono<SchemaEditResponse> {
      logger.info { "Submitting edit requests to schema server for files ${versionedSources.joinToString(", ") { it.name }}" }
      return handleFeignErrors {
         schemaEditorApi.submitEdits(
            SchemaEditRequest(packageIdentifier, versionedSources)
         )
      }

   }

   private fun doSubmit(
      generatedSource: GeneratedTaxiCode,
      validateOnly: Boolean = false,
      rawPackageIdentifier: String
   ): Mono<SchemaSubmissionResult> {
      val importRequestSourceName = "ImportRequest_${UUID.randomUUID()}"
      val packageIdentifier = PackageIdentifier.fromId(rawPackageIdentifier)
      val (messages, compiled) = validate(generatedSource, importRequestSourceName)
      val errors = messages.errors()
      if (errors.isNotEmpty()) {
         throw CompilationException(errors)
      }
      val typesInThisRequest = getCompiledElementsInSources(compiled.types, importRequestSourceName)
      val servicesInThisRequest = getCompiledElementsInSources(compiled.services, importRequestSourceName)
      val generatedThings: List<Pair<ImportableToken, List<CompilationUnit>>> =
         typesInThisRequest + servicesInThisRequest
      val (updatedSchema, versionedSources) = toVersionedSourcesAndSchema(generatedThings, compiled)
      val persist = !validateOnly
      val vyneTypes = typesInThisRequest.map { (type, _) -> updatedSchema.type(type) }
      val vyneServices = servicesInThisRequest.map { (service, _) -> updatedSchema.service(service.qualifiedName) }
      val submissionResult = SchemaSubmissionResult(
         vyneTypes.toSet(), vyneServices.toSet(), messages, generatedSource.concatenatedSource,
         dryRun = validateOnly,
         // TODO : I think this whole doSubmit() method is about to be killed,
         // so stubbing these values for now.
         SourcePackage(PackageMetadata.from(packageIdentifier), emptyList()),
         emptyList()
      )
      return if (persist) {
         submitEdits(versionedSources, packageIdentifier)
            .map { submissionResult }
      } else {
         Mono.just(submissionResult)
      }
   }

   private fun <T : Compiled> getCompiledElementsInSources(
      compiled: Set<T>,
      sourceNamePrefix: String
   ): List<Pair<T, List<CompilationUnit>>> {
      return compiled
         .mapNotNull { type ->
            val compilationUnitsInThisOperation =
               type.compilationUnits.filter { compilationUnit ->
                  compilationUnit.source.sourceName.startsWith(
                     sourceNamePrefix
                  )
               }
            if (compilationUnitsInThisOperation.isNotEmpty()) {
               type to compilationUnitsInThisOperation
            } else {
               null
            }
         }
   }

   //
   private fun validate(
      generatedSource: GeneratedTaxiCode,
      importRequestSourceName: String
   ): Pair<List<CompilationError>, TaxiDocument> {
      val sources = generatedSource.taxi.mapIndexed { index, src ->
         VersionedSource.unversioned("${importRequestSourceName}_$index", src)
      }
      // First we pre-validate with the compiler.
      // We need to do this, as we want to ensure the content is valid before writing to disk as VersionedSources.
      val (messages, compiled) = SchemaEditValidator.validate(sources, schemaStore.schema().asTaxiSchema())

      if (messages.errors().isNotEmpty()) {
         throw BadRequestException(messages.errors().joinToString("\n") { it.detailMessage })
      }
      return messages to compiled
   }

   private fun toVersionedSourcesAndSchema(
      typesAndSources: List<Pair<ImportableToken, List<CompilationUnit>>>,
      taxiDocument: TaxiDocument
   ): Pair<Schema, List<VersionedSource>> {
      val versionedSources = toVersionedSources(typesAndSources)
      return TaxiSchema(taxiDocument, listOf()) to versionedSources
   }

   private fun toVersionedSources(typesAndSources: List<Pair<ImportableToken, List<CompilationUnit>>>): List<VersionedSource> {
      // We have to work out a Type-to-file strategy.
      // As a first pass, I'm using a separate file for each type.
      // It's a little verbose on the file system, but it's a reasonable start, as it makes managing edits easier, since
      // we don't have to worry about insertions / modification within the middle of a file.
      val splitter: SourceSplitter = SingleTypePerFileSplitter
      return splitter.toVersionedSources(typesAndSources)
   }

   fun getSourcePackage(packageIdentifier: PackageIdentifier): Mono<SourcePackage> {
      return packagesServiceApi.loadPackage(packageIdentifier.uriSafeId)
         .map { it.parsedPackage.toSourcePackage() }
   }
}
