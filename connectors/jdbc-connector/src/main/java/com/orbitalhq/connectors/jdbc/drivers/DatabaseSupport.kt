package com.orbitalhq.connectors.jdbc.drivers

import com.orbitalhq.connectors.config.jdbc.DatabaseDriverName
import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcMetadataParams
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.UpsertVerb
import com.orbitalhq.connectors.jdbc.sql.dml.SqlOperation
import com.orbitalhq.schemas.AttributeName
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.DataType
import org.jooq.Field
import org.jooq.RowN

private val logger = KotlinLogging.logger {}
interface DatabaseSupport {
   val driverName: DatabaseDriverName
   val jdbcDriverMetadata: JdbcMetadataParams

   /**
    * Indicates this is used internally within Orbital, and not
    * surfaced on the UI
    */
   val isInternalDriver: Boolean
      get() = false

   fun jdbcUrlBuilder(): JdbcUrlBuilder

   /**
    * Return the schema name (if applicable) to use when listing
    * tables for the provided connection
    */
   fun schemaName(connectionConfiguration: JdbcConnectionConfiguration): String? {
      return null
   }

   /**
    * Upsert semantics vary from database to database.
    * So, individual drivers needs to provide support
    */
   fun buildUpsertStatement(
      sql: DSLContext,
      actualTableName: String,
      sqlFields: List<Field<out Any>>,
      rows: List<RowN>,
      valuesAsMaps: List<Map<AttributeName, Any?>>,
      verb: UpsertVerb,
      primaryKeyFields: List<Field<out Any>>,
      generatedFields: List<Field<out Any>>
   ): SqlOperation {
      error("Insert/Upsert not supported for ${this::class.simpleName}")
   }

   fun addAutoGeneration(dataType: DataType<out Any>, taxiType: Type): DataType<out Any> {
      return if (PrimitiveType.isNumberType(taxiType)) {
         (dataType as DataType<Int>).identity(true)
      } else {
         logger.warn { "Auto generation is not supported in primitive types of ${taxiType.toQualifiedName().typeName}" }
         dataType
      }
   }

   companion object {
      val defaultDriverRegistry = DatabaseDriverRegistry.withBuiltInDrivers()

      val drivers:List<DatabaseSupport>
         get() {
            return defaultDriverRegistry.drivers
         }

      fun forDriverName(driverName: DatabaseDriverName): DatabaseSupport {
         return defaultDriverRegistry.getDatabaseDriver(driverName)
      }
      fun register(databaseSupport: DatabaseSupport) {
         defaultDriverRegistry.addDriver(databaseSupport)
      }
   }
}

val JdbcConnectionConfiguration.databaseSupport: DatabaseSupport
   get() {
      return DatabaseSupport.forDriverName(this.driverName)
   }
