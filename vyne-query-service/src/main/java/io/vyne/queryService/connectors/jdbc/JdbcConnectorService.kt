package io.vyne.queryService.connectors.jdbc

import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.jdbc.JdbcConnectionRegistry
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.JdbcDriverConfigOptions
import io.vyne.connectors.jdbc.JdbcTable
import io.vyne.connectors.jdbc.JdbcUrlConnectionProvider
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.QualifiedName
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

private val logger = KotlinLogging.logger {}

@RestController
class JdbcConnectorService(
   private val connectionRegistry: JdbcConnectionRegistry,
   private val schemaProvider: SchemaProvider
) {

   @GetMapping("/api/connections/jdbc/drivers")
   fun listAvailableDrivers(): List<JdbcDriverConfigOptions> {
      return JdbcDriver.driverOptions
   }

   @GetMapping("/api/connections/jdbc")
   fun listConnections(): List<ConfiguredConnectionSummary> {
      return this.connectionRegistry.listAll().map { ConfiguredConnectionSummary(it.name) }
   }

   @GetMapping("/api/connections/jdbc/{connectionName}/tables")
   fun listConnectionTables(@PathVariable("connectionName") connectionName: String): List<MappedTable> {
      val template = this.connectionRegistry.getConnection(connectionName).build()
      val schema = schemaProvider.schema()
      // TODO : Write a test for this - don't merge util tested
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

   @PostMapping("/api/connections/jdbc", params = ["test=true"])
   fun testConnection(@RequestBody connectionConfig: JdbcConnectionConfiguration) {
      logger.info("Testing connection: $connectionConfig")
      try {
         val connectionProvider = JdbcUrlConnectionProvider(connectionConfig)
         val metadataService = DatabaseMetadataService(connectionProvider.build().jdbcTemplate)
         metadataService.listTables()
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
   fun createConnection(@RequestBody connectionConfig: JdbcConnectionConfiguration) {
      testConnection(connectionConfig);
      connectionRegistry.register(JdbcUrlConnectionProvider(connectionConfig))
   }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadConnectionException(message: String) : RuntimeException(message)

data class ConfiguredConnectionSummary(
   val connectionName: String
)

data class MappedTable(val table: JdbcTable, val mappedTo: QualifiedName?)
