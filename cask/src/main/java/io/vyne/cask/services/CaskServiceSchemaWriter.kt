package io.vyne.cask.services

import arrow.core.Either
import com.github.zafarkhaja.semver.Version
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.vyne.*
import io.vyne.cask.CaskSchemas
import io.vyne.schema.publisher.SchemaPublisherTransport
import lang.taxi.TaxiDocument
import lang.taxi.generators.SchemaWriter
import lang.taxi.packages.utils.log
import lang.taxi.types.ArrayType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@Component
class CaskServiceSchemaWriter(
   private val schemaPublisher: SchemaPublisherTransport,
   private val defaultCaskTypeProvider: DefaultCaskTypeProvider,
   private val schemaWriter: SchemaWriter = SchemaWriter(),
   private val caskDefinitionPublicationExecutor: ExecutorService = Executors.newSingleThreadExecutor(
      ThreadFactoryBuilder().setNameFormat("CaskServiceSchemaWriter-%d").build()
   ),
   private val identifier: PackageIdentifier = CaskSchemas.packageIdentifier
) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }


   private var currentSourcePackage: SourcePackage =
      SourcePackage(PackageMetadata.from(identifier), emptyList())

   fun write(taxiDocumentsByName: Map<String, TaxiDocument>) {
      // The rationale for not putting the types version ask the version for the cask schema is that
      // the cask schema generation logic will evolve independently of the underlying type that it's generated from.
      runOnWriterThreadAndPublish {

         val schemaVersion = getNextCandidateSchemaVersion()
         val sources = defaultCaskTypeProvider.defaultCaskTaxiTypes().plus(taxiDocumentsByName)
            .flatMap { (schemaName, taxiDocument) ->
               schemaWriter.generateSchemas(listOf(taxiDocument)).mapIndexed { index, generatedSchema ->
                  val serviceSchemaWithImports = addRequiredImportsStatements(taxiDocument, generatedSchema)
                  val versionedSourceName = if (index > 0) schemaName + index else schemaName
                  val versionedSource = VersionedSource(versionedSourceName, schemaVersion, serviceSchemaWithImports)
                  versionedSource
               }
            }
         val sourcePackage = buildCandidateSourcePackage(sources, schemaVersion)
         logger.info { "Injecting cask service schema (version=${schemaVersion})" }
         sourcePackage
      }
   }

   private fun getNextCandidateSchemaVersion(): String {
      val semver = Version.valueOf(currentSourcePackage.packageMetadata.identifier.version)
      return semver.incrementMinorVersion().toString()
   }

   private fun buildCandidateSourcePackage(
      sources: List<VersionedSource>,
      version: String = getNextCandidateSchemaVersion()
   ): SourcePackage {
      return SourcePackage(
         PackageMetadata.from(this.identifier.copy(version = version)),
         sources
      )
   }

   /**
    * Removes the versioned sources from the schema for the given types. Therefore,
    * Cask service definitions for these types will be removed from the schema.
    * @param typesForRemovedCasks List of types corresponding to deleted casks.
    */
   fun clearFromCaskSchema(typesForRemovedCasks: List<QualifiedName>) {
      val versionedSourceNamesToBeRemoved = mutableSetOf<String>()
      val currentSources = currentSourcePackage.sources.associateBy { it.name }
      runOnWriterThreadAndPublish {
         typesForRemovedCasks.forEach { typeForRemovedCask ->
            val fqn = "${DefaultCaskTypeProvider.VYNE_CASK_NAMESPACE}.${typeForRemovedCask.fullyQualifiedName}"
            versionedSourceNamesToBeRemoved.addAll(currentSources.keys.filter { key -> key.startsWith(fqn) })
         }
         log().info("Removing $versionedSourceNamesToBeRemoved from versioned sources")
         val remainingSources = currentSources.filter { (key, _) -> !versionedSourceNamesToBeRemoved.contains(key) }
            .values.toList()
         buildCandidateSourcePackage(remainingSources)
      }
   }

   /**
    * A helper to ensure that schema publication always occurs on the same thread.
    */
   private fun runOnWriterThreadAndPublish(packageGenerator: () -> SourcePackage) {
      caskDefinitionPublicationExecutor.submit {
         try {
            val sourcePackage = packageGenerator()
            val submissionResult = schemaPublisher.submitPackage(sourcePackage)
            when (submissionResult) {
               is Either.Left -> {
                  logger.error { "Failed to submit cask updates - there were compilation errors: \n${submissionResult.value.errors}" }
               }
               is Either.Right -> {
                  logger.info { "Cask sources submitted successfully, now on version ${sourcePackage.packageMetadata.identifier}" }
                  currentSourcePackage = sourcePackage
               }
            }
         } catch (e: Exception) {
            logger.error(e) { "Error in submitting schema" }
         }
      }
   }

   // TODO This should be part of schemaWriter.
   private fun addRequiredImportsStatements(taxiDocument: TaxiDocument, serviceSchema: String): String {
      val builder = StringBuilder()
      val importStatements = mutableSetOf<String>()
      val serviceTypeNames = taxiDocument.types.map { type -> type.qualifiedName }
      taxiDocument.services.forEach { service ->
         service.operations.forEach { operation ->
            val returnTypeQualifiedName =
               if (operation.returnType is ArrayType) (operation.returnType as ArrayType).type.toQualifiedName() else operation.returnType.toQualifiedName()
            val returnTypeName = returnTypeQualifiedName.fullyQualifiedName
            if (returnTypeQualifiedName.namespace.isEmpty() && !serviceTypeNames.contains(returnTypeName) && !PrimitiveType.isPrimitiveType(
                  returnTypeName
               )
            ) {
               importStatements.add("import $returnTypeName")
            }
            operation.parameters.forEach { parameter ->
               val paramTypeName = parameter.type.qualifiedName
               if (!serviceTypeNames.contains(paramTypeName) && !PrimitiveType.isPrimitiveType(paramTypeName)) {
                  importStatements.add("import $paramTypeName")
               }
            }
         }
      }
      importStatements.forEach { importStatement -> builder.appendLine(importStatement) }
      builder.appendLine()
      // replace("this:", "") is nasty hack, but Schema Write generates
      // constraint method parameters with this: e.g. - this:MaturityData >= start
      // and above fails to compile!!!
      return builder.appendLine(serviceSchema).toString().replace("this:", "")
   }
}
