package io.vyne.cask.services

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.cask.services.CaskServiceSchemaGenerator.Companion.CaskNamespacePrefix
import io.vyne.schemaStore.SchemaPublisher
import lang.taxi.TaxiDocument
import lang.taxi.generators.SchemaWriter
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
               versionedSourceMap[versionedSourceName] = versionedSource
               versionedSource
            }
         }
         log().info("Injecting cask service schema (version=${schemaVersion}): \n${schemas.joinToString(separator = "\n") { it.content }}")
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
            val fqn = "$CaskNamespacePrefix${typeForRemovedCask.fullyQualifiedName}"
            versionedSourceNamesToBeRemoved.addAll(versionedSourceMap.keys.filter { key ->  key.startsWith(fqn) })
         }
         log().warn("Removing $versionedSourceNamesToBeRemoved from versioned sources")
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
            schemaPublisher.submitSchemas(versionedSourceMap.values.toList(), schemaIdsToBeRemoved)
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
            val returnTypeName = if (operation.returnType is ArrayType) (operation.returnType as ArrayType).type.qualifiedName else operation.returnType.qualifiedName
            if (!serviceTypeNames.contains(returnTypeName) && !PrimitiveType.isPrimitiveType(returnTypeName)) {
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
