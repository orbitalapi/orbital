package io.vyne.connectors.jdbc

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.connectors.ConnectionSucceeded
import io.vyne.connectors.jdbc.schema.JdbcTaxiSchemaGenerator
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringDeserializer
import io.vyne.schemas.Schema
import lang.taxi.generators.GeneratedTaxiCode
import mu.KotlinLogging
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
   private val logger = KotlinLogging.logger {}
   fun testConnection(query: String): Either<String, ConnectionSucceeded> {
      return try {
         val map = template.queryForMap(query)
         if (map.isNotEmpty()) {
            ConnectionSucceeded.right()
         } else {
            logger.info { "Test query did not return any rows - treating as a failure" }
            "Connection succeeded, but test query returned no rows.  Possibly a bug in the adaptor?".left()
         }

      } catch (e: Exception) {
         e.message?.left() ?: "An unhandled exception occurred: ${e::class.simpleName}".left()
      }

   }

   fun listTables(): List<JdbcTable> {
      val catalog = buildCatalog()
      val tables = catalog.tables.map { table ->
         JdbcTable(table.schema.name, table.name)
      }
      return tables
   }

   fun listColumns(schemaName: String, tableName: String): List<JdbcColumn> {
       template.dataSource!!.connection.use { safeConnection ->
          val catalogPattern = null
          val (schemaPattern, tableNamePattern) = if (safeConnection.metaData.storesUpperCaseIdentifiers()) {
             schemaName.toUpperCase() to tableName.toUpperCase()
          } else {
             schemaName to tableName
          }
          val columnNamePattern = null
          val resultSet = safeConnection.metaData.getColumns(
             catalogPattern, schemaPattern, tableNamePattern, columnNamePattern
          )
          val columns = mutableListOf<JdbcColumn>()
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

   }

   fun generateTaxi(
      tables: List<TableTaxiGenerationRequest>,
      schema: Schema,
      connectionName: String
   ): GeneratedTaxiCode {
      val catalog = buildCatalog()
      return JdbcTaxiSchemaGenerator(catalog).buildSchema(tables, schema, connectionName)
   }

   private fun buildCatalog(): Catalog {
      val options = SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
         .withLoadOptions(
            LoadOptionsBuilder.builder()
               .withSchemaInfoLevel(SchemaInfoLevelBuilder.standard())
               .toOptions()
         )
      return template.dataSource!!.connection.use { safeConnection ->
         SchemaCrawlerUtility.getCatalog(safeConnection, options)
      }
   }
}

data class TableTaxiGenerationRequest(
   val table: JdbcTable,
   // Leave null to allow the API to generate a type
   val typeName: NewOrExistingTypeName? = null,
   val defaultNamespace: String? = null
)

data class NewOrExistingTypeName(
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val typeName: QualifiedName,
   val exists: Boolean
)

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
