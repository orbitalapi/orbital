package com.orbitalhq.connectors.config.jdbc

import com.orbitalhq.connectors.ConnectionDriverParam
import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.SimpleDataType

/**
 * Builds a Jdbc connection string substituting parameters
 */
interface JdbcUrlBuilder {
   val displayName: String
   val driverName: String
   val parameters: List<ConnectionDriverParam>

   fun build(inputs: Map<ConnectionParameterName, Any?>): JdbcUrlAndCredentials

}

data class ConnectionPoolProperties(val connectionTimeout: Long, val maxLifeTime: Long, val idleTimeout: Long, val minimumIdleConnections: Int, val maxPoolSize: Int)
object JdbcConnectionPoolParameters {
   private val hikariConnectionTimeout = ConnectionDriverParam("connectionPoolConnectionTimeout", SimpleDataType.NUMBER, 30000L)
   private val maxLifeTime =  ConnectionDriverParam("connectionPoolMaxLifeTime", SimpleDataType.NUMBER, 1800000L)
   private val idleTimeout = ConnectionDriverParam("connectionPoolIdleTimout", SimpleDataType.NUMBER, 600000L)
   private val minimumIdle = ConnectionDriverParam("connectionPoolMinimumIdle", SimpleDataType.NUMBER, 10)
   private val maxPoolSize = ConnectionDriverParam("connectionPoolMaxPoolSize", SimpleDataType.NUMBER, 10)
   val connectionPoolParameters: List<ConnectionDriverParam> = listOf(
      hikariConnectionTimeout,
      maxLifeTime,
      idleTimeout,
      minimumIdle,
      maxPoolSize
   )

   fun connectionPoolProperties(inputs: Map<ConnectionParameterName, Any?>): ConnectionPoolProperties {
      return ConnectionPoolProperties(
         connectionTimeout = connectionTimout(inputs),
         maxLifeTime = maxLifeTime(inputs),
         idleTimeout = idleTimeout(inputs),
         minimumIdleConnections = minimumIdleConnections(inputs),
         maxPoolSize = maxPoolSize(inputs)
      )
   }

   private fun connectionTimout(inputs: Map<ConnectionParameterName, Any?>): Long {
      return inputs[hikariConnectionTimeout.templateParamName]?.let { hikariConnectionTimeout ->
         hikariConnectionTimeout.toString().toLongOrNull()
      } ?: hikariConnectionTimeout.defaultValue!! as Long
   }

   private fun maxLifeTime(inputs: Map<ConnectionParameterName, Any?>): Long {
      return inputs[maxLifeTime.templateParamName]?.let { hikariConnectionTimeout ->
         hikariConnectionTimeout.toString().toLongOrNull()
      } ?: maxLifeTime.defaultValue!! as Long
   }

   private fun idleTimeout(inputs: Map<ConnectionParameterName, Any?>): Long {
      return inputs[idleTimeout.templateParamName]?.let { hikariConnectionTimeout ->
         hikariConnectionTimeout.toString().toLongOrNull()
      } ?: idleTimeout.defaultValue!! as Long
   }

   private fun minimumIdleConnections(inputs: Map<ConnectionParameterName, Any?>): Int {
      return inputs[minimumIdle.templateParamName]?.let { hikariConnectionTimeout ->
         hikariConnectionTimeout.toString().toIntOrNull()
      } ?: minimumIdle.defaultValue!! as Int
   }

   private fun maxPoolSize(inputs: Map<ConnectionParameterName, Any?>): Int {
      return inputs[maxPoolSize.templateParamName]?.let { hikariConnectionTimeout ->
         hikariConnectionTimeout.toString().toIntOrNull()
      } ?: maxPoolSize.defaultValue!! as Int
   }
}
