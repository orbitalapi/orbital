package io.vyne.connectors


/**
 * Intended for serving to the UI
 */
data class ConnectionDriverOptions(
   val driverName: String,
   val displayName: String,
   val parameters: List<ConnectionDriverParam>
)


/**
 * Designed to allow description of parameters in a way that a UI can build
 * a dynamic form to collect required params
 */
data class ConnectionDriverParam(
   val displayName: String,
   val dataType: SimpleDataType,
   val defaultValue: Any? = null,
   val sensitive: Boolean = false,
   val required: Boolean = true,
   val templateParamName: ConnectionParameterName = displayName,
   val allowedValues: List<Any> = emptyList()
)

typealias ConnectionParameterName = String

enum class SimpleDataType {
   STRING, NUMBER, BOOLEAN
}


/**
 * Defines a simple consistent interface to allow drivers to describe
 * the various parameters required within their driver, such that the UI can build forms for
 * their input.
 *
 * See PostgresJdbcUrlBuilder etc for a clear example
 */
interface IConnectionParameter {
   val param: ConnectionDriverParam
   val templateParamName: ConnectionParameterName
      get() = param.templateParamName
}

fun Array<out IConnectionParameter>.connectionParams(): List<ConnectionDriverParam> = this.map { it.param }


class MissingConnectionParametersException(private val parameters: List<ConnectionDriverParam>) :
   RuntimeException("The following parameters were not provided: ${parameters.joinToString { it.displayName }}")


