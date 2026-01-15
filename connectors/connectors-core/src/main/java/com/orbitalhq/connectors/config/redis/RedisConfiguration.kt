package com.orbitalhq.connectors.config.redis

import com.google.common.base.Objects
import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.config.redis.RedisConnection.REDIS_DATABASE
import com.orbitalhq.connectors.config.redis.RedisConnection.REDIS_PASSWORD
import com.orbitalhq.connectors.config.redis.RedisConnection.REDIS_SSL_ENABLED
import com.orbitalhq.connectors.config.redis.RedisConnection.REDIS_USERNAME
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorCategory
import com.orbitalhq.utils.obfuscateKeys
import kotlinx.serialization.Serializable

@Serializable
data class RedisConfiguration(
   override val connectionName: String,
   val addresses: List<String> = listOf("localhost:6379"),
   val operationCacheTtlSeconds: Int = 120,
   val connectionTimeoutSeconds: Int = 10,
   val commandTimeoutSeconds: Int = 30,
   val connectionParameters: Map<ConnectionParameterName, String> = emptyMap(),
   override val default: Boolean = false
) : ConnectorConfiguration {

   override val driverName: String = RedisConnection.DRIVER_NAME
   override val type: ConnectorCategory = ConnectorCategory.CACHE

   override fun hashCode(): Int {
      return Objects.hashCode(addresses)
   }

   override fun equals(other: Any?): Boolean {
      if (other == null) return false
      if (this.javaClass != other.javaClass) return false
      val otherConfig = other as RedisConfiguration
      return Objects.equal(this.addresses, otherConfig.addresses)
   }

   override fun getUiDisplayProperties(): Map<String, Any> {
      val baseProperties = mutableMapOf<String, Any>(
         "addresses" to addresses.joinToString(",")
      )

      // Add database if specified
      database()?.let { baseProperties["database"] = it }

      // Add username if specified (but obfuscate password)
      if (hasAuthentication()) {
         username()?.let { baseProperties[REDIS_USERNAME] = it }
         baseProperties.putAll(connectionParameters.obfuscateKeys(REDIS_PASSWORD))
      }

      // Add SSL status
      if (isSslEnabled()) {
         baseProperties[REDIS_SSL_ENABLED] = "true"
      }

      return baseProperties
   }

   fun hasAuthentication(): Boolean {
      return connectionParameters.containsKey(REDIS_PASSWORD) ||
         (connectionParameters.containsKey(REDIS_USERNAME) && connectionParameters.containsKey(REDIS_PASSWORD))
   }

   fun hasUsernamePasswordAuthentication(): Boolean {
      return connectionParameters.containsKey(REDIS_USERNAME) && connectionParameters.containsKey(REDIS_PASSWORD)
   }

   fun isSslEnabled(): Boolean {
      return connectionParameters[REDIS_SSL_ENABLED]?.toBoolean() == true
   }

   fun username(): String? = connectionParameters[REDIS_USERNAME]

   fun password(): String? = connectionParameters[REDIS_PASSWORD]

   fun database(): Int? = connectionParameters[REDIS_DATABASE]?.toIntOrNull()
}

object RedisConnection {
   const val DRIVER_NAME = "redis"
   const val REDIS_USERNAME = "username"
   const val REDIS_PASSWORD = "password"
   const val REDIS_DATABASE = "database"
   const val REDIS_SSL_ENABLED = "sslEnabled"
}
