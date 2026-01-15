package com.orbitalhq.connectors.redis

import com.orbitalhq.connectors.config.redis.RedisConfiguration
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import java.time.Duration

object RedisConnectionFactory {

   fun createConnection(config: RedisConfiguration): StatefulRedisConnection<String, String> {
      val client = createClient(config)
      return client.connect()
   }

   fun createClient(config: RedisConfiguration): RedisClient {
      // For standalone, we use the first address
      require(config.addresses.isNotEmpty()) { "At least one Redis address must be configured" }

      val address = config.addresses.first()
      val (host, port) = parseAddress(address)

      val redisUri = RedisURI.builder()
         .withHost(host)
         .withPort(port)
         .apply {
            // Set authentication
            if (config.hasUsernamePasswordAuthentication()) {
               withAuthentication(config.username()!!, config.password()!!)
            } else if (config.hasAuthentication()) {
               withPassword(config.password()!!.toCharArray())
            }

            // Set database
            config.database()?.let { withDatabase(it) }

            // Set SSL
            if (config.isSslEnabled()) {
               withSsl(true)
            }

            // Set timeouts
            withTimeout(Duration.ofSeconds(config.commandTimeoutSeconds.toLong()))
         }
         .build()

      return RedisClient.create(redisUri)
   }

   private fun parseAddress(address: String): Pair<String, Int> {
      val parts = address.split(":")
      require(parts.size == 2) { "Redis address must be in format 'host:port', got: $address" }
      val host = parts[0]
      val port = parts[1].toIntOrNull()
         ?: error("Invalid port number in Redis address: $address")
      return host to port
   }
}

interface RedisConnectionProvider {
   fun provide(config: RedisConfiguration): StatefulRedisConnection<String, String>

   /**
    * Returns the Redis connection for the provided name.
    * If the name is null, and a default connection has been configured, then
    * the default is returned - otherwise an exception is thrown
    */
   fun redisConnection(connectionName: String?): Pair<StatefulRedisConnection<String, String>, RedisConfiguration>

   /**
    * Indicates if the provider has a connection for the specified name.
    * If no name is provided, indicates if a default connection has been
    * specified
    */
   fun canProvideRedisConnection(connectionName: String?): Boolean
}

fun StatefulRedisConnection<String, String>.doHealthCheck() {
   sync().ping()
}
