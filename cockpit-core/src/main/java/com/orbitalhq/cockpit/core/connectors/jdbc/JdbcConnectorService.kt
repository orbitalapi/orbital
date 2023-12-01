package com.orbitalhq.cockpit.core.connectors.jdbc

import arrow.core.getOrHandle
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VersionedSource
import com.orbitalhq.cockpit.core.connectors.ConnectionTestedSuccessfully
import com.orbitalhq.cockpit.core.schemas.editor.LocalSchemaEditingService
import com.orbitalhq.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import com.orbitalhq.connectors.jdbc.*
import com.orbitalhq.connectors.jdbc.registry.JdbcConnectionRegistry
import com.orbitalhq.connectors.registry.ConnectionStatus
import com.orbitalhq.connectors.registry.ConnectorConfigurationSummary
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemaServer.editor.SchemaEditResponse
import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.QualifiedNameAsStringDeserializer
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.toVyneQualifiedName
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.utils.orElse
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
   private val connectionFactory: JdbcConnectionFactory,
   private val connectionRegistry: JdbcConnectionRegistry,
   private val schemaProvider: SchemaProvider,
   private val schemaEditor: LocalSchemaEditingService
) {

   // Have moved much of this to ConnectionsService


   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/jdbc/{connectionName}/tables")
   fun listConnectionTables(@PathVariable("connectionName") connectionName: String): Flux<MappedTable> {
      val template = connectionFactory.jdbcTemplate(connectionName)
      val mappedTables = DatabaseMetadataService(template.jdbcTemplate).listTables().map { table ->
         val mappedType = findTypeForTable(connectionName, table.tableName, table.schemaName)
         MappedTable(table, mappedType?.qualifiedName)
      }
      return Flux.fromIterable(mappedTables)
   }


   private fun findTypeForTable(
      connectionName: String,
      tableName: String,
      dbSchemaName: String
   ): Type? {
      val schema = schemaProvider.schema
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
      val template = connectionFactory.jdbcTemplate(connectionName)
      val tableType = findTypeForTable(connectionName, tableName, schemaName)
      val columns: List<ColumnMapping> = DatabaseMetadataService(template.jdbcTemplate)
         .listColumns(schemaName, tableName)
         .map { column -> buildColumnMapping(tableType, column) }

      return Mono.just(
         TableMetadata(
            connectionName, schemaName, tableName, tableType?.qualifiedName, columns
         )
      )
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

   @PreAuthorize("hasAuthority('${VynePrivileges.EditConnections}')")
   @DeleteMapping("/api/connections/jdbc/{connectionName}/tables/{schemaName}/{tableName}/model/{typeName}")
   fun removeTableMapping(
      @PathVariable("connectionName") connectionName: String,
      @PathVariable("schemaName") schemaName: String,
      @PathVariable("tableName") tableName: String,
      @PathVariable("typeName") typeName: String
   ): Mono<SchemaEditResponse> {
      val type = this.schemaProvider.schema
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
      return getDefaultEditorPackage().flatMap { editablePackage ->
         schemaEditor.submitEdits(
            listOf(mutatedSource), editablePackage
         )
      }
   }

   /**
    * Short-term workaround.
    * We used to only allow a single editor package, and we're working towards
    * allowing many.
    * That requires callers to indicate which package they want edits to land in.
    * For now, only a single package is allowed to be editable, but it's not defined
    * at startup anymore, so we need to query for it.
    *
    */
   private fun getDefaultEditorPackage(): Mono<PackageIdentifier> {
      return schemaEditor.getEditorConfig()
         .map { config ->
            config.getDefaultEditorPackage()
         }
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

      return getDefaultEditorPackage().flatMap { editablePackage ->
         schemaEditor.submitEdits(allEdits, editablePackage)
      }
   }


   @PreAuthorize("hasAuthority('${VynePrivileges.EditConnections}')")
   @PostMapping("/api/connections/jdbc", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: DefaultJdbcConnectionConfiguration): Mono<ConnectionStatus> {
      logger.info("Testing connection: $connectionConfig")
      return JdbcHealthCheckProvider.testConnection(connectionConfig)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditConnections}')")
   @PostMapping("/api/packages/{packageUri}/connections/jdbc")
   fun createConnection(
      @RequestBody connectionConfig: DefaultJdbcConnectionConfiguration,
      @PathVariable("packageUri2") packageUri: String
   ):
      Mono<ConnectorConfigurationSummary> {
      testConnection(connectionConfig);
      TODO("Not currently supported - need to migrate to package based writing")
//        connectionRegistry.register(connectionConfig)
//        return Mono.just(ConnectorConfigurationSummary(connectionConfig))
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
