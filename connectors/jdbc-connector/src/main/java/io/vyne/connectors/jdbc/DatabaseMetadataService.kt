package io.vyne.connectors.jdbc

import io.vyne.connectors.jdbc.schema.JdbcTaxiSchemaGenerator
import io.vyne.connectors.jdbc.schema.ServiceGeneratorConfig
import io.vyne.schemas.Schema
import org.springframework.jdbc.core.JdbcTemplate
import schemacrawler.schema.Catalog
import schemacrawler.schemacrawler.LoadOptionsBuilder
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder
import schemacrawler.tools.utility.SchemaCrawlerUtility

/**
 * Class which fetches metadata (tables, columns, datatypes)
 * from a database.
 *
 * Primarily used in UI tooling to help users build connections
 */
class DatabaseMetadataService(
   val template: JdbcTemplate
) {
   fun testConnection(): Boolean {
      listTables()
      return true
   }

   fun listTables(): List<JdbcTable> {
      val catalog = buildCatalog()
      val tables = catalog.tables.map { table ->
         JdbcTable(table.schema.name, table.name)
      }
      return tables
   }

   fun listColumns(schemaName: String, tableName: String): List<JdbcColumn> {
      val connection = template.dataSource!!.connection
      val catalogPattern = null
      val (schemaPattern, tableNamePattern) = if (connection.metaData.storesUpperCaseIdentifiers()) {
         schemaName.toUpperCase() to tableName.toUpperCase()
      } else {
         schemaName to tableName
      }
      val columnNamePattern = null
      val resultSet = connection.metaData.getColumns(
         catalogPattern, schemaPattern, tableNamePattern, columnNamePattern
      )
      var columns = mutableListOf<JdbcColumn>()
      while (resultSet.next()) {
         val columnName = resultSet.getString("COLUMN_NAME")
         val dataType = resultSet.getString("TYPE_NAME")
         val columnSize = resultSet.getInt("COLUMN_SIZE")
         val decimalDigits = resultSet.getInt("DECIMAL_DIGITS")
         val nullable = resultSet.getString("IS_NULLABLE").yesNoToBoolean()
         columns.add(
            JdbcColumn(
               columnName,
               dataType,
               columnSize,
               decimalDigits,
               nullable
            )
         )
      }
      return columns
   }

   fun generateTaxi(
      tables: List<JdbcTable>,
      namespace: String,
      schema:Schema,
      serviceGeneratorConfig: ServiceGeneratorConfig? = null
   ): List<String> {
      val catalog = buildCatalog()
      return JdbcTaxiSchemaGenerator(catalog, namespace).buildSchema(tables, schema,serviceGeneratorConfig)
   }

   private fun buildCatalog(): Catalog {
      val options = SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
         .withLoadOptions(
            LoadOptionsBuilder.builder()
               .withSchemaInfoLevel(SchemaInfoLevelBuilder.standard())
               .toOptions()
         )
      val catalog = SchemaCrawlerUtility.getCatalog(template.dataSource!!.connection, options)
      return catalog
   }
}

private fun String.yesNoToBoolean(): Boolean {
   return when (this.toLowerCase()) {
      "yes" -> true
      "no" -> false
      else -> error("$this is not a valid boolean value, expected yes/no")
   }
}

data class JdbcTable(val schemaName: String, val tableName: String)
data class JdbcColumn(
   val columnName: String, val dataType: String, val columnSize: Int,
   val decimalDigits: Int, val nullable: Boolean
)
