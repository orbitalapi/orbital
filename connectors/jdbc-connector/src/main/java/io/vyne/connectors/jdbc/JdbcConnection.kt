package io.vyne.connectors.jdbc

import io.vyne.connectors.jdbc.builders.H2JdbcUrlBuilder
import io.vyne.connectors.jdbc.builders.PostgresJdbcUrlBuilder
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource

/**
 * Main entry point for building configurable connections to the db.
 */
class JdbcUrlConnectionProvider(private val connectionConfiguration: JdbcUrlProvider) :
   JdbcConnectionProvider {
   override val name: String = connectionConfiguration.connectionName
   override val driver: String = connectionConfiguration.driver.name
   override val address: String = connectionConfiguration.buildJdbcConnectionParams().url
   override val jdbcDriver: JdbcDriver = connectionConfiguration.driver

   override fun build(): NamedParameterJdbcTemplate {
      val connectionParams = connectionConfiguration.buildJdbcConnectionParams()
      val dataSource = DriverManagerDataSource(connectionParams.url, connectionParams.username, connectionParams.password)
      return NamedParameterJdbcTemplate(dataSource)
   }
}

/**
 * Represents a persistable jdbc connection with parameters.
 * This should be used to create an actual connection to the db
 */
data class ConfigurableJdbcConnection(
   override val connectionName: String,
   override val driver: JdbcDriver,
   val connectionParameters: Map<JdbcConnectionParameterName, Any>
) : JdbcUrlProvider {
   override fun buildJdbcConnectionParams():JdbcConnectionDetails {
      return driver.urlBuilder().build(connectionParameters)
   }
}

interface JdbcUrlProvider {
   val connectionName: String
   val driver: JdbcDriver
   fun buildJdbcConnectionParams(): JdbcConnectionDetails
}

data class JdbcConnectionDetails(
   val url: String,
   val username: String?,
   val password: String?
)

/**
 * Super simple config for a pre-wired Jdbc URL.
 * Does not support parameter building in the UI.
 * Useful for tests, where the JDBC url has been constructured for us.
 */
data class JdbcUrlConnection(
   override val connectionName: String,
   override val driver: JdbcDriver,
   val connectionDetails:JdbcConnectionDetails
) : JdbcUrlProvider {
   override fun buildJdbcConnectionParams() = connectionDetails
}

/**
 * Builds a Jdbc connection string substituting parameters
 */
interface JdbcUrlBuilder {
   val displayName: String
   val driverName: String
   val parameters: List<JdbcConnectionParam>

   fun build(inputs: Map<JdbcConnectionParameterName, Any?>): JdbcConnectionDetails

   companion object {
      private fun findMissingParameters(
         parameters: List<JdbcConnectionParam>,
         inputs: Map<JdbcConnectionParameterName, Any?>
      ): List<JdbcConnectionParam> {
         return parameters.filter { it.required }
            .filter { !inputs.containsKey(it.templateParamName) || (inputs.containsKey(it.templateParamName) && inputs[it.templateParamName] == null) }
      }

      /**
       * Asserts all parameters are present, and returns
       * a map containing values populated with defaults
       * where applicable, and optional null values removed
       */
      fun assertAllParametersPresent(
         parameters: List<JdbcConnectionParam>,
         inputs: Map<JdbcConnectionParameterName, Any?>
      ): Map<JdbcConnectionParameterName, Any> {
         val missing = findMissingParameters(parameters, inputs)
         val missingWithoutDefault = missing.filter { it.defaultValue == null }
         if (missingWithoutDefault.isNotEmpty()) {
            throw MissingConnectionParametersException(missingWithoutDefault)
         }
         return (inputs.filter { it.value != null } as Map<JdbcConnectionParameterName, Any>) + missing.map { it.templateParamName to it.defaultValue!! }
      }
   }
}

class MissingConnectionParametersException(private val parameters: List<JdbcConnectionParam>) :
   RuntimeException("The following parameters were not provided: ${parameters.joinToString { it.displayName }}")

typealias JdbcConnectionString = String
typealias JdbcConnectionParameterName = String

/**
 * Designed to allow description of parameters in a way that a UI can build
 * a dynamic form to collect required params
 */
data class JdbcConnectionParam(
   val displayName: String,
   val dataType: SimpleDataType,
   val defaultValue: Any? = null,
   val sensitive: Boolean = false,
   val required: Boolean = true,
   val templateParamName: JdbcConnectionParameterName = displayName,
   val allowedValues: List<Any> = emptyList()
)

enum class SimpleDataType {
   STRING, NUMBER, BOOLEAN
}

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
   );
//   MYSQL(displayName = "MySQL", driverName = "com.mysql.jdbc.Driver");

   fun urlBuilder(): JdbcUrlBuilder {
      return this.builderFactory()
   }

   companion object {
      val driverOptions: List<JdbcDriverConfigOptions> = values().map { driver ->
         val builder = driver.urlBuilder()
         JdbcDriverConfigOptions(driver.name, builder.displayName, builder.parameters)
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
   val tableListSchemaPattern: String? = null
)

/**
 * Intended for serving to the UI
 */
data class JdbcDriverConfigOptions(
   val driverName: String,
   val displayName: String,
   val parameters: List<JdbcConnectionParam>
)
