package io.vyne.connectors.jdbc

import org.springframework.jdbc.core.JdbcTemplate

class JdbcConnectionBuilderService(
   val template: JdbcTemplate
) {
   fun listTables() {
      val connection = template.dataSource!!.connection
      val catalogPattern = null
      val schemaPattern = null
      val tableNamePattern = "%"
      val types = arrayOf("TABLE")
      val tablesResultSet = connection.metaData.getTables(
         catalogPattern, schemaPattern, tableNamePattern, types
      )
      TODO()

   }
}
