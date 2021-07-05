package io.vyne.connectors.jdbc

import org.springframework.jdbc.core.JdbcTemplate

class DatabaseMetadataService(
   val template: JdbcTemplate
) {
   fun listTables(): MutableList<JdbcTable> {
      val connection = template.dataSource!!.connection
      val catalogPattern = null
      val schemaPattern = null
      val tableNamePattern = "%"
      val types = arrayOf("TABLE")
      val tablesResultSet = connection.metaData.getTables(
         catalogPattern, schemaPattern, tableNamePattern, types
      )
      val tables = mutableListOf<JdbcTable>()
      while (tablesResultSet.next()) {
         val tableName = tablesResultSet.getString(tablesResultSet.findColumn("TABLE_NAME"))
         // TABLE_SCHEMA in H2, but also supports TABLE_SCHEM -- TABLE_SCHEM in Postgres/MySQL
         val schemaName = tablesResultSet.getString(tablesResultSet.findColumn("TABLE_SCHEM"))
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
