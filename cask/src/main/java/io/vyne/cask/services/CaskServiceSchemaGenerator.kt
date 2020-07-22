package io.vyne.cask.services

import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.ingest.DataSourceUpgradedEvent
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.types.allFields
import io.vyne.schemaStore.SchemaStore
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
   private val schemaStore: SchemaStore,
   private val caskServiceSchemaWriter: CaskServiceSchemaWriter,
   private val operationGenerators: List<OperationGenerator>,
   @Value("\${spring.application.name}") private val appName: String = "cask") {

   @EventListener
   fun onIngesterInitialised(event: IngestionInitialisedEvent) {
      log().info("Received Ingestion Initialised event ${event.type}")

      if (alreadyExists(event.type)) {
         log().info("Cask service ${caskServiceSchemaName(event.type)} already exists ")
         return
      }

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
      generateAndPublishServices(listOf(versionedType))
   }

   fun alreadyExists(versionedType: VersionedType): Boolean {
      val caskSchemaName = caskServiceSchemaName(versionedType)
      return schemaStore.schemaSet().allSources.any { it.name == caskSchemaName }
   }

   fun generateAndPublishServices(versionedTypes: List<VersionedType>) {
      val services = versionedTypes.map {
         val schemaName = caskServiceSchemaName(it)
         val taxiDocument = generateSchema(it, null)
         schemaName to taxiDocument
      }.toMap()
      caskServiceSchemaWriter.write(services)
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
      fun caskServiceSchemaName(versionedType: VersionedType):String {
         return "$CaskNamespacePrefix${versionedType.fullyQualifiedName}"
      }
   }
}


