package io.vyne.queryService.connectors.jdbc

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.VersionedSource
import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.jdbc.DefaultJdbcTemplateProvider
import io.vyne.connectors.jdbc.JdbcColumn
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.JdbcDriverConfigOptions
import io.vyne.connectors.jdbc.JdbcTable
import io.vyne.connectors.jdbc.TableTaxiGenerationRequest
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.connectors.jdbc.schema.ServiceGeneratorConfig
import io.vyne.connectors.registry.ConnectorConfigurationSummary
import io.vyne.queryService.schemas.editor.LocalSchemaEditingService
import io.vyne.queryService.schemas.editor.TaxiSubmissionResult
import io.vyne.schemaServer.editor.SchemaEditResponse
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Metadata
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringDeserializer
import io.vyne.schemas.fqn
import io.vyne.utils.orElse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
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
      val schema = schemaProvider.schema()
      return DatabaseMetadataService(template.jdbcTemplate).listTables().map { table ->
         val mappedType = schema.types
            .filter { it.hasMetadata("Table".fqn()) }
            .firstOrNull { type ->
               val metadata = type.getMetadata("Table".fqn())
               metadata.params.getOrDefault("connectionName", null) == connectionName
            }
         MappedTable(table, mappedType?.qualifiedName)
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
      val columns = DatabaseMetadataService(template.jdbcTemplate)
         .listColumns(schemaName, tableName)
      return TableMetadata(
         connectionName, schemaName, tableName, columns
      )
   }

   data class JdbcTaxiGenerationRequest(
      val tables: List<TableTaxiGenerationRequest>,
      val namespace: String
   )



   @PostMapping("/api/connections/jdbc/{connectionName}/tables/taxi/generate")
   fun generateTaxiSchema(
      @PathVariable("connectionName") connectionName: String,
      @RequestBody request: JdbcTaxiGenerationRequest
   ): Mono<TaxiSubmissionResult> {
      val connectionConfiguration = this.connectionRegistry.getConnection(connectionName)
      val template = DefaultJdbcTemplateProvider(connectionConfiguration).build()
      val taxi = DatabaseMetadataService(template.jdbcTemplate)
         .generateTaxi(
            request.tables, request.namespace, schemaProvider.schema(), ServiceGeneratorConfig(
               connectionName,
            )
         )
         .joinToString("\n")
      return schemaEditor.submit(taxi, validateOnly = true)
   }

   @PostMapping("/api/connections/jdbc/{connectionName}/tables/{schemaName}/{tableName}/model")
   fun submitModel(
      @PathVariable("connectionName") connectionName: String,
      @PathVariable("schemaName") schemaName: String,
      @PathVariable("tableName") tableName: String,
      @RequestBody request: TableModelSubmissionRequest
   ): Mono<SchemaEditResponse> {
      val versionedSources = request.columnMappings.map { columnMapping ->
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
   fun createConnection(@RequestBody connectionConfig: DefaultJdbcConnectionConfiguration) {
      testConnection(connectionConfig);
      connectionRegistry.register(connectionConfig)
   }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadConnectionException(message: String) : RuntimeException(message)

data class MappedTable(val table: JdbcTable, val mappedTo: QualifiedName?)

data class TableMetadata(
   val connectionName: String,
   val schemaName: String,
   val tableName: String,
   val columns: List<JdbcColumn>
)

data class TableModelSubmissionRequest(
   val model: TypeSpec,
   val columnMappings: List<ColumnMapping>,
   val serviceMappings: List<VersionedSource>
) {
   data class ColumnMapping(
      val name: String,
      val typeSpec: TypeSpec
   )
}

data class TypeSpec(
   // To define a pointer to an existing type...
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val typeName: QualifiedName?,
   // To map to a new type...
   val taxi: VersionedSource?,
   val metadata: List<Metadata>
)
