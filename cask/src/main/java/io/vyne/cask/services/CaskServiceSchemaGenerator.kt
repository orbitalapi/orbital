package io.vyne.cask.services

import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.OperationAnnotation
import io.vyne.cask.types.allFields
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import lang.taxi.TaxiDocument
import lang.taxi.services.Operation
import lang.taxi.services.Service
import lang.taxi.types.Annotation
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.Type
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
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
   @Async
   fun onIngesterInitialised(event: IngestionInitialisedEvent) {
      log().info("Received Ingestion Initialised event ${event.type}")

      if (alreadyExists(event.type)) {
         log().info("Cask service ${caskServiceSchemaName(event.type)} already exists ")
         return
      } else {
         generateAndPublishService(CaskTaxiPublicationRequest(
            event.type,
            registerService = true,
            registerType = false
         ))
      }
   }

   fun generateSchema(request: CaskTaxiPublicationRequest, typeMigration: TypeMigration? = null): TaxiDocument {
      val taxiType = request.type.taxiType
      // TODO Handle Type Migration.
      val fields = typeMigration?.fields ?: request.type.allFields()
      return if (taxiType is ObjectType) {
         val typesToRegister = if (request.registerType) setOf(taxiType) else emptySet()
         TaxiDocument(services = setOf(generateCaskService(fields, taxiType)), types = typesToRegister)
      } else {
         TODO("Type ${taxiType::class.simpleName} not yet supported")
      }
   }

   fun generateAndPublishService(request: CaskTaxiPublicationRequest, typeMigration: TypeMigration? = null) {
      generateAndPublishServices(listOf(request))
   }

   fun alreadyExists(versionedType: VersionedType): Boolean {
      val caskSchemaName = caskServiceSchemaName(versionedType)
      return schemaStore.schemaSet().allSources.any { it.name == caskSchemaName }
   }

   fun generateAndPublishServices(requests: List<CaskTaxiPublicationRequest>) {
      val services = requests.map { request ->
         val schemaName = caskServiceSchemaName(request.type)
         val taxiDocument = generateSchema(request, null)
         schemaName to taxiDocument
      }.toMap()
      caskServiceSchemaWriter.write(services)
   }

   private fun generateCaskService(fields: List<Field>, type: Type): Service {
      var operations: MutableList<Operation> = mutableListOf()

      operations.addAll(operationGenerators.filter { it.expectedAnnotationName() == OperationAnnotation.FindAll }.map { it.generate(null, type) })
      fields.flatMap { field ->
         operations.addAll(operationGenerators
            .filter { operationGenerator -> operationGenerator.canGenerate(field, type) }
            .map { operationGenerator -> operationGenerator.generate(field, type) })
         operations
      }

      return Service(
         qualifiedName = fullyQualifiedCaskServiceName(type),
         operations = operations,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         annotations = listOf(Annotation("ServiceDiscoveryClient", mapOf("serviceName" to appName)))
      )
   }

   companion object {
      const val CaskNamespacePrefix = "vyne.casks."
      private fun fullyQualifiedCaskServiceName(type: Type) = "$CaskNamespacePrefix${type.toQualifiedName()}CaskService"
      const val CaskApiRootPath = "/api/cask/"
      fun caskServiceSchemaName(versionedType: VersionedType): String {
         return "$CaskNamespacePrefix${versionedType.fullyQualifiedName}"
      }
   }
}
