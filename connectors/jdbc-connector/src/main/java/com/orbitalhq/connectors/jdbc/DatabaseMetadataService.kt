package com.orbitalhq.connectors.jdbc

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.orbitalhq.connectors.ConnectionSucceeded
import com.orbitalhq.connectors.jdbc.schema.JdbcTaxiSchemaGenerator
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.QualifiedNameAsStringDeserializer
import com.orbitalhq.schemas.Schema
import lang.taxi.generators.GeneratedTaxiCode
import mu.KotlinLogging
import org.springframework.core.NestedRuntimeException
import org.springframework.jdbc.core.JdbcTemplate
import schemacrawler.schema.Catalog
import schemacrawler.schemacrawler.LoadOptionsBuilder
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder
import schemacrawler.tools.utility.SchemaCrawlerUtility
import java.util.*


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
      } catch (e: NestedRuntimeException) {
         e.mostSpecificCause.message?.left() ?: "An unhandled exception occurred: ${e::class.simpleName}".left()
      } catch (e: Exception) {
         e.message?.left() ?: "An unhandled exception occurred: ${e::class.simpleName}".left()
      }

   }

   fun listTables(): List<JdbcTable> {
      val catalog = buildCatalog()
      val tables = catalog.tables.map { table ->
         val indexes = table.indexes.map {
            JdbcIndex(it.name,
               it.columns.map { indexColumn ->
                  JdbcColumn(
                     indexColumn.name,
                     indexColumn.type.name,
                     indexColumn.size,
                     indexColumn.decimalDigits,
                     indexColumn.isNullable
                  )
               })
         }
         val constraintColumns = table.primaryKey?.constrainedColumns?.map {
            JdbcColumn(
               it.name,
               it.type.name,
               it.size,
               it.decimalDigits,
               it.isNullable
            )
         } ?: emptyList()
         JdbcTable(table.schema.name, table.name, constraintColumns, indexes)
      }
      return tables
   }

   fun listColumns(schemaName: String, tableName: String): List<JdbcColumn> {
      template.dataSource!!.connection.use { safeConnection ->
         val catalogPattern = null
         val (schemaPattern, tableNamePattern) = if (safeConnection.metaData.storesUpperCaseIdentifiers()) {
            schemaName.uppercase(Locale.getDefault()) to tableName.uppercase(Locale.getDefault())
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
      return SchemaCrawlerUtility.getCatalog(
         com.orbitalhq.connectors.jdbc.schemacrawler.DataSourceConnectionSource(template.dataSource!!),
         options
      )
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
   return when (this.lowercase(Locale.getDefault())) {
      "yes" -> true
      "no" -> false
      else -> error("$this is not a valid boolean value, expected yes/no")
   }
}

data class JdbcTable(
   val schemaName: String,
   val tableName: String,
   val constrainedColumns: List<JdbcColumn> = emptyList(),
   val indexes: List<JdbcIndex> = emptyList()
)

data class JdbcColumn(
   val columnName: String, val dataType: String, val columnSize: Int,
   val decimalDigits: Int, val nullable: Boolean
)

data class JdbcIndex(val name: String, val columns: List<JdbcColumn>)