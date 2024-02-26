package com.orbitalhq.connectors.jdbc.drivers.postgres

import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.UpsertVerb
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport
import com.orbitalhq.connectors.jdbc.sql.dml.SqlOperation
import com.orbitalhq.connectors.jdbc.sql.dml.SqlQuery
import com.orbitalhq.schemas.AttributeName
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.RowN
import org.jooq.impl.DSL

object PostgresDbSupport : DatabaseSupport {
   private val logger = KotlinLogging.logger {}
   override fun jdbcUrlBuilder(): JdbcUrlBuilder = PostgresJdbcUrlBuilder()

   override fun buildUpsertStatement(
      sql: DSLContext,
      actualTableName: String,
      sqlFields: List<Field<out Any>>,
      rows: List<RowN>,
      valuesAsMaps: List<Map<AttributeName, Any?>>,
      verb: UpsertVerb,
      primaryKeyFields: List<Field<out Any>>,
      generatedFields: List<Field<out Any>>
   ): SqlOperation {
      // TODO : Support for explicit Insert / Update.
      // Currently verything is upsert
      val statement = sql.insertInto(DSL.table(DSL.name(actualTableName)), *sqlFields.toTypedArray())
         .valuesOfRows(*rows.toTypedArray())
         .let { insert ->
            if (primaryKeyFields.isNotEmpty()) {
               insert.onConflict(primaryKeyFields)
                  .doUpdate().setAllToExcluded()
                  .returningResult(generatedFields)
            } else {
               insert.returningResult(generatedFields)
            }
         }
      return SqlQuery(statement, returnsValues = generatedFields.isNotEmpty())
   }

}
