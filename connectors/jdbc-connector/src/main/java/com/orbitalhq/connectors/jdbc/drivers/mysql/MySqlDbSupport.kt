package com.orbitalhq.connectors.jdbc.drivers.mysql

import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.UpsertVerb
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport
import com.orbitalhq.connectors.jdbc.sql.dml.SqlOperation
import com.orbitalhq.connectors.jdbc.sql.dml.SqlQuery
import com.orbitalhq.schemas.AttributeName
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.RowN
import org.jooq.impl.DSL

/**
 * Documenting known issues:
 *
 * ## DDL
 *  - When generating a table, if the column type is string, and the column is used in an index,
 *    then the size must be set. We default to VARCHAR(255), which cannot be changed currently
 *
 *  - MySQL doesn't support create index if not exists, so we've had to fall back to create index.
 *    This means that the creation may fail if the index already exists
 *
 * ## DML
 *  - MySQL (and therefore JOOQ) doesn't support UPDATE ... RETURNING syntaxes. This means on an
 *    upsert, we are unable to find the id of the created entry (see related jOOQ issue)
 */
object MySqlDbSupport : DatabaseSupport{
   override fun jdbcUrlBuilder(): JdbcUrlBuilder = MySqlJdbcUrlBuilder

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

      // Note:
      // MySQL (and therefore jOOQ) doesn't support UPDATE ... RETURNING.
      // As a result, currently when we do an UPSERT, we can get the affected row count,
      // but not the affected rows.
      // See: https://github.com/jOOQ/jOOQ/issues/6865
      val statement = sql.insertInto(DSL.table(DSL.name(actualTableName)), *sqlFields.toTypedArray())
         .valuesOfRows(*rows.toTypedArray())
         .let { insert ->
            if (primaryKeyFields.isNotEmpty()) {
               insert.onConflict(primaryKeyFields)
                  .doUpdate().setAllToExcluded()
//                  .returningResult(generatedFields)
            } else {
               insert
//               insert.returningResult(generatedFields)
            }
         }
      return SqlQuery(statement, returnsValues = false)
   }
}
