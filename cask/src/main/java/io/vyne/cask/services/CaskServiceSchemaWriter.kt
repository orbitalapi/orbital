package io.vyne.cask.services

import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.utils.log
import lang.taxi.TaxiDocument
import lang.taxi.generators.SchemaWriter
import lang.taxi.types.ArrayType
import lang.taxi.types.PrimitiveType
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class CaskServiceSchemaWriter(private val schemaPublisher: SchemaPublisher, private val schemaWriter: SchemaWriter = SchemaWriter()) {
   private val generationCounter: AtomicInteger = AtomicInteger(0)

   fun write(map: Map<String, TaxiDocument>) {
      // The rationale for not putting the types version ask the version for the cask schema is that
      // the cask schema generation logic will evolve independently of the underlying type that it's generated from.
      val schemaVersion = "1.0.${generationCounter.incrementAndGet()}"
      val schemas = map.map { (schemaName, taxiDocument) ->
         val serviceSchema = schemaWriter.generateSchemas(listOf(taxiDocument)).first()
         val serviceSchemaWithImports = addRequiredImportsStatements(taxiDocument, serviceSchema)
         VersionedSource(schemaName, schemaVersion, serviceSchemaWithImports)
      }.toList()

      log().info("Injecting cask service schema (version=${schemaVersion}): \n${schemas.map{it.content}.joinToString(separator = "\n")}")

      try {
         schemaPublisher.submitSchemas(schemas)
      } catch (e: Exception) {
         log().error("Error in submitting schema", e)
      }

   }

   // TODO This should be part of schemaWriter.
   private fun addRequiredImportsStatements(taxiDocument: TaxiDocument, serviceSchema: String): String {
      val builder = StringBuilder()
      val importStatements = mutableSetOf<String>()
      val serviceTypeNames = taxiDocument.types.map { type -> type.qualifiedName }
      taxiDocument.services.forEach { service ->
         service.operations.forEach { operation ->
            val returnTypeName =  if (operation.returnType is ArrayType) (operation.returnType as ArrayType).type.qualifiedName else operation.returnType.qualifiedName
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
