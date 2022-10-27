package io.vyne.connectors.jdbc.sql.ddl

import io.vyne.connectors.jdbc.JdbcUrlCredentialsConnectionConfiguration
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.connectors.jdbc.sqlBuilder
import io.vyne.schemas.Type
import org.jooq.CreateViewFinalStep
import org.jooq.DSLContext

/**
 * Generates, and optionally executes a CREATE OR REPLACE VIW
 * statement for the target type
 */
class ViewGenerator {
   fun execute(type: Type, targetTableName: String, dsl: DSLContext): Int {
      val statement = generate(type, targetTableName, dsl)
      return statement.execute()
   }

   fun generate(type: Type, targetTableName: String, dsl: DSLContext): CreateViewFinalStep {
      val tableName = SqlUtils.tableNameOrTypeName(type.taxiType)
      return dsl.createOrReplaceView(tableName)
         .`as`("SELECT * FROM ${targetTableName};")
   }

   /**
    * Generates a create or replace view statement, without requiring a db connection.
    *
    * This isn't directly executable, as it's
    * disconnected from the underlying data connection.
    * Useful for testing
    */
   fun generateStatementOnly(
      type: Type,
      targetTableName: String,
      connectionDetails: JdbcUrlCredentialsConnectionConfiguration
   ): CreateViewFinalStep {
      return generate(type, targetTableName, connectionDetails.sqlBuilder())
   }
}
