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
         val schemaName = tablesResultSet.getString(tablesResultSet.findColumn("TABLE_SCHEMA"))
         tables.add(JdbcTable(schemaName, tableName))
         // What else do we care about?
      }
      return tables
   }

   fun listColumns(schemaName: String, tableName: String): List<JdbcColumn> {
      val connection = template.dataSource!!.connection
      val catalogPattern = null
      val schemaPattern = schemaName.toUpperCase() // Uppercase allows unquoted strings, according to H2 docs
      val tableNamePatterh = tableName.toUpperCase() // as above
      val columNamePattern = null
      val resultSet = connection.metaData.getColumns(
         catalogPattern, schemaPattern, tableNamePatterh, columNamePattern
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
