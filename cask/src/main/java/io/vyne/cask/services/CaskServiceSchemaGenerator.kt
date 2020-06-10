package io.vyne.cask.services

import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.ingest.DataSourceUpgradedEvent
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.types.allFields
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import lang.taxi.TaxiDocument
import lang.taxi.services.Service
import lang.taxi.types.Annotation
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.Type
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Generates the service schema for the given cask data schema.
 */
@Component
class CaskServiceSchemaGenerator(
   private val schemaProvider: SchemaProvider,
   private val caskServiceSchemaWriter: CaskServiceSchemaWriter,
   private val operationGenerators: List<OperationGenerator>,
   @Value("\${spring.application.name}") private val appName: String = "cask") {

   @EventListener
   fun onIngesterInitialised(event: IngestionInitialisedEvent) {
      log().info("Received Ingestion Initialised event ${event.type}")
      generateAndPublishService(event.type)
   }

   @EventListener
   fun onDataSourceUpgraded(event: DataSourceUpgradedEvent) {
      log().info("Received Data Source Upgraded event {}", event)
      // TODO
   }

   fun generateSchema(versionedType: VersionedType, typeMigration: TypeMigration? = null): TaxiDocument {
      val taxiType = versionedType.taxiType
      // TODO Handle Type Migration.
      val fields = typeMigration?.fields ?: versionedType.allFields()
      return if (taxiType is ObjectType) {
         TaxiDocument(services = setOf(generateCaskService(fields, taxiType)), types = setOf())
      } else {
         TODO("Type ${taxiType::class.simpleName} not yet supported")
      }
   }

   fun generateAndPublishService(versionedType: VersionedType, typeMigration: TypeMigration? = null) {
      val serviceName = fullyQualifiedCaskServiceName(versionedType.taxiType)
      if (schemaProvider.schema().hasService(serviceName)) {
         log().info("Service ${serviceName} already exists!")
         return
      }
      val caskSchema = generateSchema(versionedType, typeMigration)
      caskServiceSchemaWriter.write(caskSchema, versionedType)
   }

   private fun generateCaskService(fields: List<Field>, type: Type) = Service(
      qualifiedName = fullyQualifiedCaskServiceName(type),
      operations = fields.flatMap { field ->
         operationGenerators
            .filter { operationGenerator -> operationGenerator.canGenerate(field, type) }
            .map { operationGenerator -> operationGenerator.generate(field, type) }
      },
      compilationUnits = listOf(CompilationUnit.unspecified()),
      annotations = listOf(Annotation("ServiceDiscoveryClient", mapOf("serviceName" to appName)))
   )

   companion object {
      const val CaskNamespacePrefix = "vyne.casks."
      private fun fullyQualifiedCaskServiceName(type: Type) = "$CaskNamespacePrefix${type.toQualifiedName()}CaskService"
      const val CaskApiRootPath = "/api/cask/"
      private fun operationReturnTypeQualifiedName(type: Type) = "$CaskNamespacePrefix${type.toQualifiedName().typeName}List"
   }
}


