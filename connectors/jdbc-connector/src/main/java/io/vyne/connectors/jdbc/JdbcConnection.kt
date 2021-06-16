package io.vyne.connectors.jdbc

import io.vyne.connectors.jdbc.builders.H2JdbcUrlBuilder
import io.vyne.connectors.jdbc.builders.PostgresJdbcUrlBuilder
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource

/**
 * Main entry point for building configurable connections to the db.
 */
class JdbcUrlConnectionProvider(private val connectionConfiguration: JdbcConnectionConfiguration) :
   JdbcConnectionProvider {
   override val name: String = connectionConfiguration.connectionName
   override fun build(): NamedParameterJdbcTemplate {
      val jdbcUrl = connectionConfiguration.buildJdbcUrl();
      val dataSource = DriverManagerDataSource(jdbcUrl)
      return NamedParameterJdbcTemplate(dataSource)
   }
}

/**
 * Represents a persistable jdbc connection with parameters.
 * This should be used to create an actual connection to the db
 */
data class JdbcConnectionConfiguration(
   val connectionName: String,
   val driver: JdbcDriver,
   val connectionParameters: Map<JdbcConnectionParameterName, Any>
) {
   fun buildJdbcUrl(): String {
      return driver.urlBuilder().build(connectionParameters)
   }
}

interface JdbcUrlBuilder {
   val displayName: String
   val driverName: String
   val parameters: List<JdbcConnectionParam>

   fun build(inputs: Map<JdbcConnectionParameterName, Any?>): JdbcConnectionString

   companion object {
      private fun findMissingParameters(
         parameters: List<JdbcConnectionParam>,
         inputs: Map<JdbcConnectionParameterName, Any?>
      ): List<JdbcConnectionParam> {
         return parameters.filter { it.required }
            .filter { !inputs.containsKey(it.templateParamName) || (inputs.containsKey(it.templateParamName) && inputs[it.templateParamName] == null)}
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
         return (inputs.filter { it.value != null } as Map<JdbcConnectionParameterName,Any>) + missing.map { it.templateParamName to it.defaultValue!! }
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
enum class JdbcDriver(private val builderFactory: () -> JdbcUrlBuilder) {
   H2(builderFactory = { H2JdbcUrlBuilder() }),
   POSTGRES(builderFactory = { PostgresJdbcUrlBuilder() });
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
 * Intended for serving to the UI
 */
data class JdbcDriverConfigOptions(val driverName: String, val displayName: String, val parameters: List<JdbcConnectionParam>)
