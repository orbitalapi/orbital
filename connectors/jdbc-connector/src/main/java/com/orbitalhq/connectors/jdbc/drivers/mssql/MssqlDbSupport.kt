package com.orbitalhq.connectors.jdbc.drivers.mssql

import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.UpsertVerb
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport
import com.orbitalhq.connectors.jdbc.sql.dml.SqlBatch
import com.orbitalhq.connectors.jdbc.sql.dml.SqlOperation
import com.orbitalhq.connectors.jdbc.sql.dml.SqlQuery
import com.orbitalhq.schemas.AttributeName
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Param
import org.jooq.Record
import org.jooq.RowN
import org.jooq.Table
import org.jooq.impl.DSL.*

object MssqlDbSupport : DatabaseSupport {
   override fun jdbcUrlBuilder(): JdbcUrlBuilder = MssqlJdbcUrlBuilder()

   override fun schemaName(connectionConfiguration: JdbcConnectionConfiguration): String? {
      val schemaName = connectionConfiguration.getConnectionParameterOrNull(MssqlJdbcUrlBuilder.Parameters.SCHEMA)
         ?: return null
      val dbName = connectionConfiguration.getConnectionParameterOrNull(MssqlJdbcUrlBuilder.Parameters.DATABASE)
         ?: return null
      return "${dbName}.${schemaName}"
   }


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

      val table = table(name(actualTableName))
      return when (verb) {
         UpsertVerb.Insert -> insert(sql, table, sqlFields, rows, generatedFields)
         UpsertVerb.Update -> {
            val updateQueries = valuesAsMaps.map { rowAsMap ->
               require(primaryKeyFields.isNotEmpty()) { "Cannot perform update on $actualTableName as no primary keys exist" }

               val conditions = primaryKeyFields.map { field ->
                  require(rowAsMap.containsKey(field.name)) { "Cannot perform update on $actualTableName as primary key ${field.name} was not provided" }
                  (field as Field<Any>).eq(rowAsMap[field.name]!!)
               }

               val query = sql.update(table)
                  .set(rowAsMap.withoutPrimaryKeys(primaryKeyFields))
                  .where(*conditions.toTypedArray())
               query

            }

            SqlBatch.from(sql, updateQueries)
         }

         UpsertVerb.Upsert -> {
            TODO("Support upserts on MSSQL")
//            if (verb == UpsertVerb.Upsert && rows.size > 1) {
//               error("Upsert not support on MSSQL for a batch yet.")
//            }
         }
      }
   }

   private fun insert(
      sql: DSLContext,
      table: Table<Record>,
      sqlFields: List<Field<out Any>>,
      rows: List<RowN>,
      generatedFields: List<Field<out Any>>
   ): SqlOperation {
      val query = sql.insertInto(table, *sqlFields.toTypedArray())
         .valuesOfRows(rows)
         .returningResult(generatedFields)
      return SqlQuery(query, returnsValues = true)
   }

}

private fun  Map<String, Any?>.withoutPrimaryKeys(primaryKeyFields: List<Field<out Any>>):Map<String,Any?> {
   return this.filter { (fieldName, _) -> primaryKeyFields.none { it.name == fieldName } }
}
