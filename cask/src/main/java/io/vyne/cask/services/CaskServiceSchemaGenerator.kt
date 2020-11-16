package io.vyne.cask.services

import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.query.DefaultOperationGenerator
import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.OperationAnnotation
import io.vyne.cask.types.allFields
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.VersionedType
import lang.taxi.TaxiDocument
import lang.taxi.services.Operation
import lang.taxi.services.Service
import lang.taxi.types.Annotation
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.Type
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Generates the service schema for the given cask data schema.
 */
@Component
class CaskServiceSchemaGenerator(
   private val schemaStore: SchemaStore,
   private val caskServiceSchemaWriter: CaskServiceSchemaWriter,
   private val operationGenerators: List<OperationGenerator>,
   private val defaultOperationGenerators: List<DefaultOperationGenerator>,
   @Value("\${cask.service.annotations:#{null}}") private val caskServiceAnnotations: String? = null,
   @Value("\${spring.application.name}") private val appName: String = "cask"
   ) {
   private val serviceAnnotations =
      listOf(Annotation("ServiceDiscoveryClient", mapOf("serviceName" to appName))) +
         (caskServiceAnnotations?.split(",")?.map { Annotation(it.trim()) } ?: listOf())



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
      val defaultOperations = defaultOperationGenerators.map {defaultOperationGenerators -> defaultOperationGenerators.generate(type) }
      operations.addAll(defaultOperations)
      fields.flatMap { field ->
         operations.addAll(operationGenerators
            .filter { operationGenerator -> operationGenerator.canGenerate(field, type) }
            .map { operationGenerator -> operationGenerator.generate(field, type) })
         operations
      }

      return Service(
         qualifiedName = fullyQualifiedCaskServiceName(type),
         members = operations,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         annotations = serviceAnnotations
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
