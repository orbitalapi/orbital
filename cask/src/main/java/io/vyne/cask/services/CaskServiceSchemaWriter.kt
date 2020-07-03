package io.vyne.cask.services

import io.vyne.cask.services.CaskServiceSchemaGenerator.Companion.CaskNamespacePrefix
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import lang.taxi.TaxiDocument
import lang.taxi.generators.SchemaWriter
import lang.taxi.types.ArrayType
import lang.taxi.types.PrimitiveType
import org.springframework.stereotype.Component
import java.lang.StringBuilder

@Component
class CaskServiceSchemaWriter(private val schemaStoreClient: SchemaStoreClient, private val schemaWriter: SchemaWriter = SchemaWriter()) {
   fun write(taxiDocument: TaxiDocument, versionedType: VersionedType) {
      val serviceSchema = addRequiredImportsStatements(taxiDocument, schemaWriter.generateSchemas(listOf(taxiDocument)).first())
      log().info("injecting cask service schema for $versionedType: \n$serviceSchema")
      try {
         schemaStoreClient.submitSchema(
            caskServiceSchemaName(versionedType),
            CaskServiceSchemaVersion,
            serviceSchema)
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

   companion object {
      // The rationale for not putting the types version ask the version for the cask schema is that
      // the cask schema generation logic will evolve independently of the underlying type that it's generated from.
      private const val CaskServiceSchemaVersion = "1.0.0"
      private fun caskServiceSchemaName(versionedType: VersionedType):String {
         return "$CaskNamespacePrefix${versionedType.versionedName}"
      }
   }
}
