package io.vyne.cask.query

import io.vyne.cask.query.CaskServiceSchemaGenerator.Companion.CaskNamespacePrefix
import lang.taxi.types.Type
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import lang.taxi.TaxiDocument
import lang.taxi.generators.SchemaWriter
import org.springframework.stereotype.Component

@Component
class CaskServiceSchemaWriter(private val schemaStoreClient: SchemaStoreClient, private val schemaWriter: SchemaWriter = SchemaWriter()) {
   fun write(taxiDocument: TaxiDocument, versionedType: VersionedType, type: Type) {
      val serviceSchema = schemaWriter.generateSchemas(listOf(taxiDocument)).first()
      log().info("injecting cask service schema for {} as {}", type.toQualifiedName(), serviceSchema)
      try {
         schemaStoreClient.submitSchema(
            caskServiceSchemaName(type, versionedType.versionedName),
            CaskServiceSchemaVersion,
            serviceSchema)
      } catch(e: Exception) {
         log().error("Error in submitting schema", e)
      }
   }
   companion object {
      // The rationale for not putting the types version ask the version for the cask schema is that
      // the cask schema generation logic will evolve independently of the underlying type that it's generated from.
      private const val CaskServiceSchemaVersion = "1.0.0"
      private fun caskServiceSchemaName(type: Type, versionName: String) = "$CaskNamespacePrefix${type.toQualifiedName()}-$versionName"
   }
}
