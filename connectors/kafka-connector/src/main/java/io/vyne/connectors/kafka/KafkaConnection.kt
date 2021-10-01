package io.vyne.connectors.kafka

import io.vyne.connectors.registry.ConnectorConfiguration
import io.vyne.connectors.registry.ConnectorType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource

/**
 * Main entry point for building configurable connections to the db.
 */
class DefaultKafkaTemplateProvider(private val connectionConfiguration: KafkaConnectionConfiguration)  {
//   override val name: String = connectionConfiguration.connectionName
//   override val driver: String = connectionConfiguration.jdbcDriver.name
//   override val address: String = connectionConfiguration.buildUrlAndCredentials().url
//   override val jdbcDriver: JdbcDriver = connectionConfiguration.jdbcDriver


}

/**
 * Represents a persistable kafka connection with parameters.
 * This should be used to create an actual connection to kafka
 */
data class DefaultKafkaConnectionConfiguration(
   override val connectionName: String,
   // connectionParameters must be typed as Map<String,String> (rather than <String,Any>
   // as the Hocon persistence library we're using can't deserialize values from disk into
   // an Any.  If this causes issues, we'll need to wrap the deserialization to coerce numbers from strings.
   val connectionParameters: Map<KafkaConnectionParameterName, String>,

) : KafkaConnectionConfiguration {
   companion object {
      fun forParams(
         connectionName: String,
         connectionParameters: Map<IKafkaConnectionParamEnum, String>
      ): DefaultKafkaConnectionConfiguration {
         return DefaultKafkaConnectionConfiguration(
            connectionName,
            connectionParameters.mapKeys { it.key.templateParamName }
         )
      }
   }

}

interface KafkaConnectionConfiguration : ConnectorConfiguration {
   override val driverName: String
      get() = "kafka"

   override val address: String
      get() = "kafka"

   override val connectionName: String

   override val type: ConnectorType
      get() = ConnectorType.KAFKA
}



class MissingConnectionParametersException(private val parameters: List<KafkaConnectionParam>) :
   RuntimeException("The following parameters were not provided: ${parameters.joinToString { it.displayName }}")

typealias KafkaConnectionParameterName = String

/**
 * Designed to allow description of parameters in a way that a UI can build
 * a dynamic form to collect required params
 */
data class KafkaConnectionParam(
   val displayName: String,
   val dataType: SimpleDataType,
   val defaultValue: Any? = null,
   val sensitive: Boolean = false,
   val required: Boolean = true,
   val templateParamName: KafkaConnectionParameterName = displayName,
   val allowedValues: List<Any> = emptyList()
)

// What am I trying to do here?
interface IKafkaConnectionParamEnum {
   val param: KafkaConnectionParam
   val templateParamName: KafkaConnectionParameterName
      get() = param.templateParamName
}

fun Array<out IKafkaConnectionParamEnum>.connectionParams(): List<KafkaConnectionParam> = this.map { it.param }

enum class SimpleDataType {
   STRING, NUMBER, BOOLEAN
}

