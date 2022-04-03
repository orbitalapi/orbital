package io.vyne.connectors.jdbc

import io.vyne.connectors.ConnectionDriverOptions
import io.vyne.connectors.ConnectionDriverParam
import io.vyne.connectors.ConnectionParameterName
import io.vyne.connectors.jdbc.builders.H2JdbcUrlBuilder
import io.vyne.connectors.jdbc.builders.PostgresJdbcUrlBuilder
import io.vyne.connectors.jdbc.builders.RedshiftJdbcUrlBuilder
import io.vyne.connectors.jdbc.builders.SnowflakeJdbcUrlBuilder
import io.vyne.connectors.registry.ConnectorType


/**
 * Enum of supported Jdbc drivers.
 */
enum class JdbcDriver(
   private val builderFactory: () -> JdbcUrlBuilder,
   val metadata: JdbcMetadataParams = JdbcMetadataParams()
) {
   H2(
      builderFactory = { H2JdbcUrlBuilder() },
      metadata = JdbcMetadataParams(
         tableListSchemaPattern = "PUBLIC"
      )
   ),
   POSTGRES(
      builderFactory = { PostgresJdbcUrlBuilder() },
      metadata = JdbcMetadataParams().copy(
         tableTypesToListTables = arrayOf("TABLE")
      )
   ),
   SNOWFLAKE(
      builderFactory = { SnowflakeJdbcUrlBuilder() },
      metadata = JdbcMetadataParams().copy(
         tableTypesToListTables = arrayOf("TABLE")
      )
   ),
   REDSHIFT(
      builderFactory = { RedshiftJdbcUrlBuilder() },
      metadata = JdbcMetadataParams().copy(
         tableTypesToListTables = arrayOf("TABLE")
      )
   );
//   MYSQL(displayName = "MySQL", driverName = "com.mysql.jdbc.Driver");

   fun urlBuilder(): JdbcUrlBuilder {
      return this.builderFactory()
   }

   companion object {
      val driverOptions: List<ConnectionDriverOptions> = values().map { driver ->
         val builder = driver.urlBuilder()
         ConnectionDriverOptions(driver.name, builder.displayName, ConnectorType.JDBC, builder.parameters)
      }

   }
}


/**
 * This class provides a way to capture the subtle
 * differences between each JDBC driver implementation
 * when fetching metadata.
 *
 * We try to provide reasonable defaults, and then let
 * each driver override as necessary.
 */
@Suppress("ArrayInDataClass")
data class JdbcMetadataParams(
   /*
    * This is the value to pass when doing a jdbc call
    * to list all tables.
    * It varies between drivers, in Postgres it's "TABLE",
    * whereas for H2 it's null.
    * Null is a valid option, and indicates not to perform a filter.
    */
   val tableTypesToListTables: Array<String>? = null,

   /**
    * When iterating the results of metaData.getTables(...)
    * which column contains the name of the table?
    */
   val tableListTableNameColumn: String = "TABLE_NAME",

   /**
    * When iterating the results of metaData.getTables(...)
    * which column conains the name of the schema?
    */
   val tableListSchemaNameColumn: String = "TABLE_SCHEM", // Not a typo, _SCHEM tested against H2 and Postgres

   /**
    * When listing tables in metadata.getTables(...) use this schema pattern.
    * Null is a reasonable default here
    */
   val tableListSchemaPattern: String? = null,


   /**
    * A query to use to test the connection is valid.
    * Should be fast, and require minimal db permissions
    */
   val testQuery: String = "SELECT 1"
)


/**
 * Builds a Jdbc connection string substituting parameters
 */
interface JdbcUrlBuilder {
   val displayName: String
   val driverName: String
   val parameters: List<ConnectionDriverParam>

   fun build(inputs: Map<ConnectionParameterName, Any?>): JdbcUrlAndCredentials

}
