package io.vyne.connectors.jdbc

import org.springframework.jdbc.core.JdbcTemplate

/**
 * Class which fetches metadata (tables, columns, datatypes)
 * from a database.
 *
 * Primarily used in UI tooling to help users build connections
 */
class DatabaseMetadataService(
   val template: JdbcTemplate,
   val driver:JdbcDriver
) {
   fun testConnection():Boolean {
      listTables()
      return true
   }
   fun listTables(): MutableList<JdbcTable> {
      val connection = template.dataSource!!.connection
      val catalogPattern = null
      val schemaPattern = driver.metadata.tableListSchemaPattern
      val tableNamePattern = "%"

      val tablesResultSet = connection.metaData.getTables(
         catalogPattern, schemaPattern, tableNamePattern, driver.metadata.tableTypesToListTables
      )
      val tables = mutableListOf<JdbcTable>()
      while (tablesResultSet.next()) {
         val tableName = tablesResultSet.getString(tablesResultSet.findColumn(driver.metadata.tableListTableNameColumn))
         val schemaName = tablesResultSet.getString(tablesResultSet.findColumn(driver.metadata.tableListSchemaNameColumn))
         tables.add(JdbcTable(schemaName, tableName))
         // What else do we care about?
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
