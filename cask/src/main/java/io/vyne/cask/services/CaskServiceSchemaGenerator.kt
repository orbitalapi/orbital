package io.vyne.cask.services

import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.query.DefaultOperationGenerator
import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.types.allFields
import io.vyne.query.graph.ServiceAnnotations
import io.vyne.query.graph.ServiceParams
import io.vyne.schemaConsumerApi.SchemaStore
import io.vyne.schemas.VersionedType
import lang.taxi.TaxiDocument
import lang.taxi.services.Service
import lang.taxi.services.ServiceMember
import lang.taxi.types.Annotation
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.QualifiedName
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
   private val defaultCaskTypeProvider: DefaultCaskTypeProvider,
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
      val withCaskInsertedAtType = defaultCaskTypeProvider.withDefaultCaskTaxiType(taxiType)
      return if (taxiType is ObjectType) {
         val typesToRegister = if (request.registerType) setOf(taxiType, withCaskInsertedAtType) else setOf(withCaskInsertedAtType)
         TaxiDocument(services = setOf(generateCaskService(fields, taxiType, request.excludedCaskServices)), types = typesToRegister)
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

   private fun generateCaskService(fields: List<Field>, type: Type, excludedCaskServices: Set<QualifiedName>): Service {
      val operations: MutableList<ServiceMember> = mutableListOf()
      val defaultOperations = defaultOperationGenerators
         .filter { it.canGenerate(type) }
         .map {defaultOperationGenerators -> defaultOperationGenerators.generate(type) }


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
         annotations = decorateDatasourceAnnotation(excludedCaskServices)
      )
   }

   private fun decorateDatasourceAnnotation(excludedCaskServices: Set<QualifiedName>): List<Annotation> {
      return if (excludedCaskServices.isNotEmpty() && this.serviceAnnotations.map { it.name }.contains(ServiceAnnotations.Datasource.annotation)) {
         val datasourceAnnotation =
            Annotation(
               ServiceAnnotations.Datasource.annotation,
               mapOf<String, Any?>(ServiceParams.Exclude.paramName to listOf(excludedCaskServices.map { it.fullyQualifiedName })))
         serviceAnnotations.filterNot { it.name ==  ServiceAnnotations.Datasource.annotation}.plus(datasourceAnnotation)
      } else {
         this.serviceAnnotations
      }
   }

   companion object {
      private fun fullyQualifiedCaskServiceName(type: Type) = "${DefaultCaskTypeProvider.VYNE_CASK_NAMESPACE}.${type.toQualifiedName()}CaskService"
      fun fullyQualifiedCaskServiceName(qualifiedName: String) = "${DefaultCaskTypeProvider.VYNE_CASK_NAMESPACE}.${qualifiedName}CaskService"
      const val CaskApiRootPath = "/api/cask/"
      fun caskServiceSchemaName(versionedType: VersionedType): String {
         return caskServiceSchemaName(versionedType.fullyQualifiedName)
      }

      fun caskServiceSchemaName(qualifiedName: String): String {
         return "${DefaultCaskTypeProvider.VYNE_CASK_NAMESPACE}.$qualifiedName"
      }
   }
}
