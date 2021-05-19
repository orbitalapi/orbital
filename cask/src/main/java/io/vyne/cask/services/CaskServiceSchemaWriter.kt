package io.vyne.cask.services

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaPublisher
import lang.taxi.TaxiDocument
import lang.taxi.generators.SchemaWriter
import lang.taxi.types.ArrayType
import lang.taxi.types.PrimitiveType
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
   private val versionedSourceMap: MutableMap<String, VersionedSource> = mutableMapOf()

   fun write(taxiDocumentsByName: Map<String, TaxiDocument>) {
      // The rationale for not putting the types version ask the version for the cask schema is that
      // the cask schema generation logic will evolve independently of the underlying type that it's generated from.
      caskDefinitionPublicationExecutor.submit {
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

         logger.info { "Injecting cask service schema (version=${schemaVersion})" }
         logger.debug { schemas.map { it.content }.joinToString(separator = "\n") }

         try {
            schemaPublisher.submitSchemas(versionedSourceMap.values.toList())
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
      importStatements.forEach { importStatement -> builder.appendln(importStatement) }
      builder.appendln()
      // replace("this:", "") is nasty hack, but Schema Write generates
      // constraint method parameters with this: e.g. - this:MaturityData >= start
      // and above fais to compile!!!
      return builder.appendln(serviceSchema).toString().replace("this:", "")
   }
}
