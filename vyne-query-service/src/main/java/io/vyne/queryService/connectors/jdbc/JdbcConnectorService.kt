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
import io.vyne.security.VynePrivileges
import io.vyne.utils.orElse
import lang.taxi.generators.SchemaWriter
import lang.taxi.types.Annotatable
import lang.taxi.types.Annotation
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@RestController
class JdbcConnectorService(
   private val connectionRegistry: JdbcConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val schemaEditor: LocalSchemaEditingService
) {

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/jdbc/drivers")
   fun listAvailableDrivers(): Flux<JdbcDriverConfigOptions> {
      return Flux.fromIterable(JdbcDriver.driverOptions)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/jdbc")
   fun listConnections(): Flux<ConnectorConfigurationSummary> {
      return Flux.fromIterable(this.connectionRegistry.listAll().map {
         ConnectorConfigurationSummary(it)
      })
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/jdbc/{connectionName}/tables")
   fun listConnectionTables(@PathVariable("connectionName") connectionName: String): Flux<MappedTable> {
      val connection = this.connectionRegistry.getConnection(connectionName)
      val template = DefaultJdbcTemplateProvider(connection).build()
      val mappedTables = DatabaseMetadataService(template.jdbcTemplate).listTables().map { table ->
         val mappedType = findTypeForTable(connectionName, table.tableName, table.schemaName)
         MappedTable(table, mappedType?.qualifiedName)
      }
      return Flux.fromIterable(mappedTables)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/jdbc/{connectionName}")
   fun getConnection(@PathVariable("connectionName") connectionName: String): Mono<ConnectorConfigurationSummary> {
      val summary = this.connectionRegistry.getConnection(connectionName).let { connection -> ConnectorConfigurationSummary(connection) }
      return Mono.just(summary)
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

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/jdbc/{connectionName}/tables/{schemaName}/{tableName}/metadata")
   fun getTableMetadata(
      @PathVariable("connectionName") connectionName: String,
      @PathVariable("schemaName") schemaName: String,
      @PathVariable("tableName") tableName: String
   ): Mono<TableMetadata> {
      val connectionConfiguration = this.connectionRegistry.getConnection(connectionName)
      val template = DefaultJdbcTemplateProvider(connectionConfiguration).build()
      val tableType = findTypeForTable(connectionName, tableName, schemaName)
      val columns: List<ColumnMapping> = DatabaseMetadataService(template.jdbcTemplate)
         .listColumns(schemaName, tableName)
         .map { column -> buildColumnMapping(tableType, column) }

      return Mono.just(TableMetadata(
         connectionName, schemaName, tableName, tableType?.qualifiedName, columns
      ))
   }

   private fun buildColumnMapping(
      tableType: Type?,
      column: JdbcColumn
   ): ColumnMapping {
      val mappedColumnTypeSpec: TypeSpec? = tableType?.let {
         tableType.attributes.entries.firstOrNull { (name, _) -> name == column.columnName }?.value
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


   @PreAuthorize("hasAuthority('${VynePrivileges.EditConnections}')")
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

   @PreAuthorize("hasAuthority('${VynePrivileges.EditConnections}')")
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


   @PreAuthorize("hasAuthority('${VynePrivileges.EditConnections}')")
   @PostMapping("/api/connections/jdbc", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: DefaultJdbcConnectionConfiguration): Mono<Unit> {
      logger.info("Testing connection: $connectionConfig")
      try {
         val connectionProvider = DefaultJdbcTemplateProvider(connectionConfig)
         val metadataService = DatabaseMetadataService(connectionProvider.build().jdbcTemplate)
         metadataService.testConnection()
         return Mono.just(Unit)
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

   @PreAuthorize("hasAuthority('${VynePrivileges.EditConnections}')")
   @PostMapping("/api/connections/jdbc")
   fun createConnection(@RequestBody connectionConfig: DefaultJdbcConnectionConfiguration):
      Mono<ConnectorConfigurationSummary> {
      testConnection(connectionConfig);
      connectionRegistry.register(connectionConfig)
      return Mono.just(ConnectorConfigurationSummary(connectionConfig))
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
