package io.vyne.connectors.jdbc

import io.vyne.connectors.jdbc.builders.H2JdbcConnectionBuilder
import io.vyne.connectors.jdbc.builders.PostgresJdbcConnectionBuilder
import lang.taxi.types.PrimitiveType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

data class JdbcConnectionDetails(
   val name: String,
   val driver: JdbcDriver,
   val auth: JdbcAuthentication
)

interface JdbcAuthentication {
   // for polymorphic serialization / deserialization
   val type: Type

   enum class Type {
      UsernameAndPassword
   }
}

data class UsernameAndPasswordAuthentication(
   val username: String,
   val password: String
) {
   val kind = JdbcAuthentication.Type.UsernameAndPassword
}

interface JdbcConnectionBuilder {
   val displayName: String
   val driverName: String
   val parameters: List<JdbcConnectionParam>

   fun build(inputs: Map<JdbcConnectionParameterName, Any>): JdbcConnectionString

   companion object {
      private fun findMissingParameters(
         parameters: List<JdbcConnectionParam>,
         inputs: Map<JdbcConnectionParameterName, Any>
      ): List<JdbcConnectionParam> {
         return parameters.filter { it.required }
            .filter { !inputs.containsKey(it.templateParamName) }
      }

      /**
       * Asserts all parameters are present, and returns
       * a map containing values populated with defaults
       * where applicable
       */
      fun assertAllParametersPresent(
         parameters: List<JdbcConnectionParam>,
         inputs: Map<JdbcConnectionParameterName, Any>
      ): Map<JdbcConnectionParameterName, Any> {
         val missing = findMissingParameters(parameters, inputs)
         val missingWithoutDefault = missing.filter { it.defaultValue == null }
         if (missingWithoutDefault.isNotEmpty()) {
            throw MissingConnectionParametersException(missingWithoutDefault)
         }
         return inputs + missing.map { it.templateParamName to it.defaultValue!! }
      }
   }
}

class MissingConnectionParametersException(val parameters: List<JdbcConnectionParam>) :
   RuntimeException("The following parameters were not provided: ${parameters.joinToString { it.displayName }}")

typealias JdbcConnectionString = String
typealias JdbcConnectionParameterName = String

data class JdbcConnectionParam(
   val displayName: String,
   val dataType: PrimitiveType,
   val defaultValue: Any? = null,
   val sensitive: Boolean = false,
   val required: Boolean = true,
   val templateParamName: JdbcConnectionParameterName = displayName
)

enum class JdbcDriver(private val builderFactory: () -> JdbcConnectionBuilder) {
   H2(builderFactory = { H2JdbcConnectionBuilder() }),
   POSTGRES(builderFactory = { PostgresJdbcConnectionBuilder() });
//   MYSQL(displayName = "MySQL", driverName = "com.mysql.jdbc.Driver");

   fun newBuilder(): JdbcConnectionBuilder {
      return this.builderFactory()
   }
}

data class JdbcConnection(val name: String, val template: NamedParameterJdbcTemplate)

class JdbcConnectionRegistry(connections: List<JdbcConnection>) {
   private val connections: MutableMap<String, JdbcConnection> = connections.associateBy { it.name }.toMutableMap()

   fun hasConnection(name: String): Boolean = connections.containsKey(name)

   fun getConnection(name: String): JdbcConnection =
      connections[name] ?: error("No JdbcConnection with name $name is registered")
}


