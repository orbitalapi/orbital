package com.orbitalhq.connectors.jdbc.sql.ddl

import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.SqlUtils
import com.orbitalhq.connectors.jdbc.sqlBuilder
import com.orbitalhq.schemas.Type
import org.jooq.CreateViewFinalStep
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.table

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
      return dsl.createOrReplaceView(name(tableName))
         .`as`(DSL.select(DSL.asterisk()).from(table(name(targetTableName))))
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
