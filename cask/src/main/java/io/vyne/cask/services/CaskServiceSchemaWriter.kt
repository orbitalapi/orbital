package io.vyne.cask.services

import arrow.core.Either
import arrow.core.extensions.list.functorFilter.filter
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schema.publisher.SchemaPublisher
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
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

@Component
class CaskServiceSchemaWriter(
    private val schemaPublisher: SchemaPublisher,
    private val defaultCaskTypeProvider: DefaultCaskTypeProvider,
    private val schemaWriter: SchemaWriter = SchemaWriter(),
    private val caskDefinitionPublicationExecutor: ExecutorService = Executors.newSingleThreadExecutor(ThreadFactoryBuilder().setNameFormat("CaskServiceSchemaWriter-%d").build())) {
   private val generationCounter: AtomicInteger = AtomicInteger(0)

   /**
    * Map of versioned sources contributed to the overall schema by Cask
    * Please make sure that this map is mutated on caskDefinitionPublicationExecutor,
    * so use runOnWriterThreadAndPublish function to mutate and publish.
    */
   private val versionedSourceMap: MutableMap<String, VersionedSource> = mutableMapOf()
   private val submittedCaskSources: MutableList<VersionedSource> = mutableListOf()

   fun write(taxiDocumentsByName: Map<String, TaxiDocument>) {
      // The rationale for not putting the types version ask the version for the cask schema is that
      // the cask schema generation logic will evolve independently of the underlying type that it's generated from.
      runOnWriterThreadAndPublish {
         val schemaVersion = "1.0.${generationCounter.incrementAndGet()}"
         val schemas = defaultCaskTypeProvider.defaultCaskTaxiTypes().plus(taxiDocumentsByName).flatMap { (schemaName, taxiDocument) ->
            schemaWriter.generateSchemas(listOf(taxiDocument)).mapIndexed { index, generatedSchema ->
               val serviceSchemaWithImports = addRequiredImportsStatements(taxiDocument, generatedSchema)
               val versionedSourceName = if (index > 0) schemaName + index else schemaName
               VersionedSource(versionedSourceName, schemaVersion, serviceSchemaWithImports)
               val versionedSource = VersionedSource(versionedSourceName, schemaVersion, serviceSchemaWithImports)
               if (submittedCaskSources.firstOrNull { submittedVersionedSources ->
                     submittedVersionedSources.name == versionedSource.name && submittedVersionedSources.contentHash == versionedSource.contentHash
                  } == null) {
                  versionedSourceMap[versionedSourceName] = versionedSource
               }
               versionedSource
            }
         }
         logger.info { "Injecting cask service schema (version=${schemaVersion})"}
         emptyList()
      }
   }

   /**
    * Removes the versioned sources from the schema for the given types. Therefore,
    * Cask service definitions for these types will be removed from the schema.
    * @param typesForRemovedCasks List of types corresponding to deleted casks.
    */
   fun clearFromCaskSchema(typesForRemovedCasks: List<QualifiedName>) {
      val versionedSourceNamesToBeRemoved = mutableListOf<SchemaId>()
      runOnWriterThreadAndPublish {
         typesForRemovedCasks.forEach { typeForRemovedCask ->
            val fqn = "${DefaultCaskTypeProvider.VYNE_CASK_NAMESPACE}.${typeForRemovedCask.fullyQualifiedName}"
            versionedSourceNamesToBeRemoved.addAll(versionedSourceMap.keys.filter { key ->  key.startsWith(fqn) })
         }
         log().info("Removing $versionedSourceNamesToBeRemoved from versioned sources")
         val versionedSourcesToBeRemoved = versionedSourceNamesToBeRemoved.mapNotNull { versionedSourceMap[it] }
         versionedSourceMap.keys.removeAll(versionedSourceNamesToBeRemoved)
         versionedSourcesToBeRemoved.map { it.id }
      }
   }

   /**
    * A helper to ensure that schema publication always occurs on the same thread.
    * @param functor that supposed to mutate versionedSourceMap and return list of schemaIds to be removed from the current schema (if any)
    */
   private fun runOnWriterThreadAndPublish(functor: () -> List<SchemaId>) {
      caskDefinitionPublicationExecutor.submit {
         val schemaIdsToBeRemoved = functor()
         try {
            val caskVersionedSources = versionedSourceMap.values.toList()
            val submittedSources = schemaPublisher.submitSchemas(caskVersionedSources, schemaIdsToBeRemoved)
               .map { schema -> schema.sources.filter { caskVersionedSources.contains(it) } }

            if (submittedSources is Either.Right) {
               submittedCaskSources.clear()
               submittedCaskSources.addAll(submittedSources.b)
            } else {
               submittedCaskSources.clear()
            }

         } catch (e: Exception) {
            logger.error(e) {"Error in submitting schema" }
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
            val returnTypeQualifiedName = if (operation.returnType is ArrayType) (operation.returnType as ArrayType).type.toQualifiedName() else operation.returnType.toQualifiedName()
            val returnTypeName = returnTypeQualifiedName.fullyQualifiedName
            if (returnTypeQualifiedName.namespace.isEmpty() && !serviceTypeNames.contains(returnTypeName) && !PrimitiveType.isPrimitiveType(returnTypeName)) {
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
