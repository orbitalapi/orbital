package io.vyne.connectors.jdbc

import io.vyne.connectors.jdbc.builders.H2JdbcUrlBuilder
import io.vyne.connectors.jdbc.builders.PostgresJdbcUrlBuilder
import io.vyne.connectors.jdbc.builders.RedshiftJdbcUrlBuilder
import io.vyne.connectors.jdbc.builders.SnowflakeJdbcUrlBuilder
import io.vyne.connectors.jdbc.registry.JdbcTemplateProvider
import io.vyne.connectors.registry.ConnectorConfiguration
import io.vyne.connectors.registry.ConnectorType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource

/**
 * Main entry point for building configurable connections to the db.
 */
class DefaultJdbcTemplateProvider(private val connectionConfiguration: JdbcConnectionConfiguration) :
   JdbcTemplateProvider {
//   override val name: String = connectionConfiguration.connectionName
//   override val driver: String = connectionConfiguration.jdbcDriver.name
//   override val address: String = connectionConfiguration.buildUrlAndCredentials().url
//   override val jdbcDriver: JdbcDriver = connectionConfiguration.jdbcDriver

   override fun build(): NamedParameterJdbcTemplate {
      val connectionParams = connectionConfiguration.buildUrlAndCredentials()
      val dataSource =
         DriverManagerDataSource(connectionParams.url, connectionParams.username, connectionParams.password)
      return NamedParameterJdbcTemplate(dataSource)
   }
}

/**
 * Represents a persistable jdbc connection with parameters.
 * This should be used to create an actual connection to the db
 */
data class DefaultJdbcConnectionConfiguration(
   override val connectionName: String,
   override val jdbcDriver: JdbcDriver,
   // connectionParameters must be typed as Map<String,String> (rather than <String,Any>
   // as the Hocon persistence library we're using can't deserialize values from disk into
   // an Any.  If this causes issues, we'll need to wrap the deserialization to coerce numbers from strings.
   val connectionParameters: Map<JdbcConnectionParameterName, String>
) : JdbcConnectionConfiguration {
   companion object {
      fun forParams(
         connectionName: String,
         driver: JdbcDriver,
         connectionParameters: Map<IJdbcConnectionParamEnum, String>
      ): DefaultJdbcConnectionConfiguration {
         return DefaultJdbcConnectionConfiguration(
            connectionName,
            driver,
            connectionParameters.mapKeys { it.key.templateParamName })
      }
   }

   override val address: String = buildUrlAndCredentials().url

   override fun buildUrlAndCredentials(): JdbcUrlAndCredentials {
      return jdbcDriver.urlBuilder().build(connectionParameters)
   }
}

interface JdbcConnectionConfiguration : ConnectorConfiguration {
   override val connectionName: String
   val jdbcDriver: JdbcDriver
   fun buildUrlAndCredentials(): JdbcUrlAndCredentials

   override val driverName: String
      get() = jdbcDriver.name
   override val type: ConnectorType
      get() = ConnectorType.JDBC
}

data class JdbcUrlAndCredentials(
   val url: String,
   val username: String?,
   val password: String?
)

/**
 * Super simple config for a pre-wired Jdbc URL.
 * Does not support parameter building in the UI.
 * Useful for tests, where the NamedTemplate has already been constructured for us
 */
data class NamedTemplateConnection(
   override val connectionName: String,
   val template: NamedParameterJdbcTemplate,
   override val jdbcDriver: JdbcDriver = JdbcDriver.H2
) : JdbcConnectionConfiguration {
   override fun buildUrlAndCredentials(): JdbcUrlAndCredentials {
      val metadata = template.jdbcTemplate.dataSource.connection.metaData
      return JdbcUrlAndCredentials(
         metadata.url,
         metadata.userName,
         ""
      )
   }

   override val address: String = buildUrlAndCredentials().url
}

/**
 * Another test class, useful where we want to shortcut the config, because the
 * JDBC Url has already been explitily provided to us
 */
data class JdbcUrlCredentialsConnectionConfiguration(
   override val connectionName: String,
   override val jdbcDriver: JdbcDriver,
   val urlAndCredentials: JdbcUrlAndCredentials
) : JdbcConnectionConfiguration {
   override fun buildUrlAndCredentials() = urlAndCredentials

   override val address: String = urlAndCredentials.url

}


/**
 * Builds a Jdbc connection string substituting parameters
 */
interface JdbcUrlBuilder {
   val displayName: String
   val driverName: String
   val parameters: List<JdbcConnectionParam>

   fun build(inputs: Map<JdbcConnectionParameterName, Any?>): JdbcUrlAndCredentials

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

// What am I trying to do here?
interface IJdbcConnectionParamEnum {
   val param: JdbcConnectionParam
   val templateParamName: JdbcConnectionParameterName
      get() = param.templateParamName
}

fun Array<out IJdbcConnectionParamEnum>.connectionParams(): List<JdbcConnectionParam> = this.map { it.param }

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
