package com.orbitalhq.connectors.jdbc.drivers.h2

import com.orbitalhq.connectors.config.jdbc.JdbcMetadataParams
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.UpsertVerb
import com.orbitalhq.connectors.config.jdbc.DatabaseDriverName
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport
import com.orbitalhq.connectors.jdbc.drivers.postgres.PostgresDbSupport
import com.orbitalhq.connectors.jdbc.sql.dml.SqlOperation
import com.orbitalhq.schemas.AttributeName
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.RowN

object H2DatabaseSupport : DatabaseSupport {
   override val driverName: DatabaseDriverName = "H2"
   override fun jdbcUrlBuilder(): JdbcUrlBuilder = H2JdbcUrlBuilder()
   override val jdbcDriverMetadata: JdbcMetadataParams = JdbcMetadataParams(
      tableTypesToListTables = null,
      tableListSchemaPattern = "PUBLIC"
   )

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
      return PostgresDbSupport.buildUpsertStatement(
         sql,
         actualTableName,
         sqlFields,
         rows,
         valuesAsMaps,
         verb,
         primaryKeyFields,
         generatedFields
      )
   }
}
