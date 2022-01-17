package io.vyne.queryService.connectors.jdbc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.VersionedSource
import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.jdbc.DefaultJdbcTemplateProvider
import io.vyne.connectors.jdbc.JdbcColumn
import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.JdbcDriverConfigOptions
import io.vyne.connectors.jdbc.JdbcTable
import io.vyne.connectors.jdbc.TableTaxiGenerationRequest
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.connectors.registry.ConnectorConfigurationSummary
import io.vyne.queryService.schemas.editor.LocalSchemaEditingService
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemaServer.editor.SchemaEditResponse
import io.vyne.schemas.Field
import io.vyne.schemas.Metadata
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringDeserializer
import io.vyne.schemas.Type
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.utils.orElse
import lang.taxi.generators.SchemaWriter
import lang.taxi.types.Annotatable
import lang.taxi.types.Annotation
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@RestController
class JdbcConnectorService(
   private val connectionRegistry: JdbcConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val schemaEditor: LocalSchemaEditingService
) {

   @GetMapping("/api/connections/jdbc/drivers")
   fun listAvailableDrivers(): List<JdbcDriverConfigOptions> {
      return JdbcDriver.driverOptions
   }

   @GetMapping("/api/connections/jdbc")
   fun listConnections(): List<ConnectorConfigurationSummary> {
      return this.connectionRegistry.listAll().map {
         ConnectorConfigurationSummary(it)
      }
   }

   @GetMapping("/api/connections/jdbc/{connectionName}/tables")
   fun listConnectionTables(@PathVariable("connectionName") connectionName: String): List<MappedTable> {
      val connection = this.connectionRegistry.getConnection(connectionName)
      val template = DefaultJdbcTemplateProvider(connection).build()
      return DatabaseMetadataService(template.jdbcTemplate).listTables().map { table ->
         val mappedType = findTypeForTable(connectionName, table.tableName, table.schemaName)
         MappedTable(table, mappedType?.qualifiedName)
      }
   }

   private fun findTypeForTable(
      connectionName: String,
      tableName: String,
      dbSchemaName: String
   ): Type? {
      val schema = schemaProvider.schema()
      return schema.types
         .filter { it.hasMetadata(JdbcConnectorTaxi.Annotations.tableName.toVyneQualifiedName()) }
         .firstOrNull { type ->
            val metadata = type.getMetadata(JdbcConnectorTaxi.Annotations.tableName.toVyneQualifiedName())
            metadata.params.getOrDefault("connection", null) == connectionName &&
               metadata.params.getOrDefault("table", null) == tableName &&
               metadata.params.getOrDefault("schema", null) == dbSchemaName
         }
   }

   @GetMapping("/api/connections/jdbc/{connectionName}/tables/{schemaName}/{tableName}/metadata")
   fun getTableMetadata(
      @PathVariable("connectionName") connectionName: String,
      @PathVariable("schemaName") schemaName: String,
      @PathVariable("tableName") tableName: String
   ): TableMetadata {
      val connectionConfiguration = this.connectionRegistry.getConnection(connectionName)
      val template = DefaultJdbcTemplateProvider(connectionConfiguration).build()
      val tableType = findTypeForTable(connectionName, tableName, schemaName)
      val columns: List<ColumnMapping> = DatabaseMetadataService(template.jdbcTemplate)
         .listColumns(schemaName, tableName)
         .map { column -> buildColumnMapping(tableType, column) }
      return TableMetadata(
         connectionName, schemaName, tableName, tableType?.qualifiedName, columns
      )
   }

   private fun buildColumnMapping(
      tableType: Type?,
      column: JdbcColumn
   ): ColumnMapping {
      val mappedColumnTypeSpec: TypeSpec? = tableType?.let {
         tableType.attributes.entries.firstOrNull { (name, type) -> name == column.columnName }?.value
            ?.let { field: Field ->
               TypeSpec(typeName = field.type, metadata = field.metadata, taxi = null)
            }
      }
      return ColumnMapping(
         column.columnName,
         mappedColumnTypeSpec,
         column
      )
   }

   data class JdbcTaxiGenerationRequest(
      val tables: List<TableTaxiGenerationRequest>,
   )


   @DeleteMapping("/api/connections/jdbc/{connectionName}/tables/{schemaName}/{tableName}/model/{typeName}")
   fun removeTableMapping(
      @PathVariable("connectionName") connectionName: String,
      @PathVariable("schemaName") schemaName: String,
      @PathVariable("tableName") tableName: String,
      @PathVariable("typeName") typeName: String
   ): Mono<SchemaEditResponse> {
      val type = this.schemaProvider.schema()
         .type(typeName)

      if (type.sources.size > 1) {
         error("This type contains multiple sources.  Editing is not yet supported")
      }

      // Filter out the @Table annotation
      val annotationFilter = { annotatable: Annotatable, annotation: Annotation ->
         annotation.qualifiedName != JdbcConnectorTaxi.Annotations.tableName.fullyQualifiedName
      }
      val taxi = SchemaWriter(annotationFilter).generateTaxi(type.taxiType)
      val mutatedSource = VersionedSource(
         type.sources.single().name,
         type.sources.single().version, // TODO, we should increment this...
         taxi
      )
      return this.schemaEditor.submitEdits(
         listOf(mutatedSource)
      )
   }

   @PostMapping("/api/connections/jdbc/{connectionName}/tables/{schemaName}/{tableName}/model")
   fun submitModel(
      @PathVariable("connectionName") connectionName: String,
      @PathVariable("schemaName") schemaName: String,
      @PathVariable("tableName") tableName: String,
      @RequestBody request: TableModelSubmissionRequest
   ): Mono<SchemaEditResponse> {
      val versionedSources = request.columnMappings.map { columnMapping ->
         require(columnMapping.typeSpec != null) { "A typeSpec property was not specified for column ${columnMapping.name}" }
         if (columnMapping.typeSpec.taxi == null) {
            error("Only taxi based mappings are currently supported")
         }
         columnMapping.typeSpec.taxi
      }
      val modelSource = request.model.taxi ?: error("Only taxi based mappings are currently supported")
      val allEdits = versionedSources + modelSource + request.serviceMappings
      return schemaEditor.submitEdits(allEdits)
   }


   @PostMapping("/api/connections/jdbc", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: DefaultJdbcConnectionConfiguration) {
      logger.info("Testing connection: $connectionConfig")
      try {
         val connectionProvider = DefaultJdbcTemplateProvider(connectionConfig)
         val metadataService = DatabaseMetadataService(connectionProvider.build().jdbcTemplate)
         metadataService.testConnection()
      } catch (e: Exception) {
         val cause = e.cause?.let { cause ->
            val errorType = cause::class.simpleName!!.replace("java.net.", "")
            "$errorType : ${cause.message?.orElse("No other details provided")}"
         } ?: "No other details provided"
         // Clean up driver specific junk
         val cleanedMessage = e.message!!.removePrefix("FATAL:")
            .trim()
         val message = "$cleanedMessage.  Cause: $cause"
         throw BadConnectionException(message)
      }
   }

   @PostMapping("/api/connections/jdbc")
   fun createConnection(@RequestBody connectionConfig: DefaultJdbcConnectionConfiguration):ConnectorConfigurationSummary {
      testConnection(connectionConfig);
      connectionRegistry.register(connectionConfig)
      return ConnectorConfigurationSummary(connectionConfig)
   }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadConnectionException(message: String) : RuntimeException(message)

data class MappedTable(val table: JdbcTable, val mappedTo: QualifiedName?)

data class TableMetadata(
   val connectionName: String,
   val schemaName: String,
   val tableName: String,
   val mappedType: QualifiedName?,
   val columns: List<ColumnMapping>
)

data class TableModelSubmissionRequest(
   val model: TypeSpec,
   val columnMappings: List<ColumnMapping>,
   val serviceMappings: List<VersionedSource>
)

data class ColumnMapping(
   val name: String,
   // Null when sending to the UI for an unmapped table
   val typeSpec: TypeSpec?,
   // columnSpec is null / ignored when sending up to the server,
   // but populated when sending to the UI
   @JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val columnSpec: JdbcColumn? = null
)

data class TypeSpec(
   // To define a pointer to an existing type...
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val typeName: QualifiedName?,
   // To map to a new type...
   val taxi: VersionedSource?,
   val metadata: List<Metadata>
)
