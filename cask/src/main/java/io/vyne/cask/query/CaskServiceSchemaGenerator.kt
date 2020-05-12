package io.vyne.cask.query

import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.ingest.DataSourceUpgradedEvent
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.types.allFields
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import lang.taxi.TaxiDocument
import lang.taxi.services.Operation
import lang.taxi.services.Parameter
import lang.taxi.services.Service
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import lang.taxi.types.TypeAlias
import lang.taxi.types.TypeAliasDefinition
import org.apache.commons.lang3.StringUtils
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
   @Value("\${spring.application.name}") private val appName: String = "cask") {

   @EventListener
   fun onIngesterInitialised(event: IngestionInitialisedEvent) {
      log().info("Received Ingestion Initialised event {}", event.type)
      generate(event.type)
   }

   @EventListener
   fun onDataSourceUpgraded(event: DataSourceUpgradedEvent) {
      log().info("Received Data Source Upgraded event {}", event)
      // TODO
   }

   fun generate(versionedType: VersionedType, typeMigration: TypeMigration? = null) {
      val schema = schemaProvider.schema()
      val type = schema.toTaxiType(versionedType)
      // TODO Handle Type Migration.
      val fields = typeMigration?.fields ?: versionedType.allFields()
      if (type is ObjectType) {
         val taxiDocument = TaxiDocument(services = setOf(generateFindByFields(fields, type)), types = setOf(operationReturnType(type)))
         caskServiceSchemaWriter.write(taxiDocument, versionedType, type)
      } else {
         TODO("Type ${type::class.simpleName} not yet supported")
      }
   }

   private fun operationReturnType(type: Type) = TypeAlias(
      operationReturnTypeQualifiedName(type),
      TypeAliasDefinition(ArrayType(
         type = type,
         source = CompilationUnit.unspecified(), inheritsFrom = setOf()), compilationUnit = CompilationUnit.unspecified())
   )

   private fun generateFindByFields(fields: List<Field>, type: Type) = Service(
      qualifiedName = fullyQualifiedCaskServiceName(type),
      operations = fields.map { field -> findByFieldOperation(field, type) },
      compilationUnits = listOf(CompilationUnit.unspecified()),
      annotations = listOf(Annotation("ServiceDiscoveryClient", mapOf("serviceName" to appName)))
   )

   private fun findByFieldOperation(field: Field, type: Type): Operation {
      if (PrimitiveType.isAssignableToPrimitiveType(field.type)) {
         val parameter = Parameter(
            annotations = listOf(Annotation("PathVariable", mapOf("name" to field.name))),
            type = field.type,
            name = field.name,
            constraints = listOf())

         return Operation(
            name = "findBy${StringUtils.capitalize(field.name)}",
            parameters = listOf(parameter),
            annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getFindByIdRestPath(type, field)))),
            returnType = operationReturnType(type),
            compilationUnits = listOf(CompilationUnit.unspecified())
         )
      } else {
         TODO("not implemented")
      }
   }

   companion object {
      const val CaskNamespacePrefix = "vyne.casks."
      private fun fullyQualifiedCaskServiceName(type: Type) = "$CaskNamespacePrefix${type.toQualifiedName()}CaskService"
      private fun getFindByIdRestPath(type: Type, field: Field): String {
         val typeQualifiedName = type.toQualifiedName()
         val fieldTypeQualifiedName = field.type.toQualifiedName()
         val path = AttributePath.from(typeQualifiedName.toString())
         return "$CaskApiRootPath${path.parts.joinToString("/")}/${field.name}/{$fieldTypeQualifiedName}"
      }
      const val CaskApiRootPath = "/api/cask/"
      private fun operationReturnTypeQualifiedName(type: Type) = "$CaskNamespacePrefix${type.toQualifiedName().typeName}List"
   }
}


