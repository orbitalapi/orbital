package com.orbitalhq.connectors.config.hazelcast

import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.config.hazelcast.HazelcastConnection.HAZELCAST_CLIENT_NAME
import com.orbitalhq.connectors.config.hazelcast.HazelcastConnection.HAZELCAST_CLUSTER_NAME
import com.orbitalhq.connectors.config.hazelcast.HazelcastConnection.HAZELCAST_ENTERPRISE_PASSWORD
import com.orbitalhq.connectors.config.hazelcast.HazelcastConnection.HAZELCAST_ENTERPRISE_USERNAME
import com.orbitalhq.connectors.config.hazelcast.HazelcastConnection.VIRIDIAN_DISCOVERY_TOKEN
import com.orbitalhq.connectors.config.hazelcast.HazelcastConnection.VIRIDIAN_KEY_STORE_PASSWORD
import com.orbitalhq.connectors.config.hazelcast.HazelcastConnection.VIRIDIAN_TRUST_STORE_PASSWORD
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import com.orbitalhq.utils.obfuscateKeys
import kotlinx.serialization.Serializable
import java.util.Properties

@Serializable
class HazelcastConfiguration(
   override val connectionName: String,
   val addresses: List<String> = listOf(),
   val operationCacheTtlSeconds: Int = 120,
   val connectionParameters: Map<ConnectionParameterName, String> = emptyMap()
) : ConnectorConfiguration {

   override val driverName: String = HazelcastConnection.DRIVER_NAME
   override val type: ConnectorType = ConnectorType.CACHE
   override fun getUiDisplayProperties(): Map<String, Any> {
      return when {
         addresses.isEmpty() && connectionParameters.containsKey(VIRIDIAN_DISCOVERY_TOKEN) -> connectionParameters.obfuscateKeys(
            VIRIDIAN_DISCOVERY_TOKEN,
            VIRIDIAN_KEY_STORE_PASSWORD,
            VIRIDIAN_TRUST_STORE_PASSWORD
         )

         addresses.isNotEmpty() &&
            connectionParameters.containsKey(HAZELCAST_ENTERPRISE_USERNAME) &&
            connectionParameters.containsKey(HAZELCAST_ENTERPRISE_PASSWORD) -> connectionParameters.obfuscateKeys(
            HAZELCAST_ENTERPRISE_PASSWORD
         ) + mapOf("addresses" to addresses.joinToString(","),
            HAZELCAST_ENTERPRISE_USERNAME to username()!!,
            HAZELCAST_CLUSTER_NAME to hazelcastClusterName()!!)

         else -> mapOf("addresses" to addresses.joinToString(","))

      }
   }

   fun userNamePasswordAuthentication() = connectionParameters.containsKey(HAZELCAST_ENTERPRISE_USERNAME)
      && connectionParameters.containsKey(HAZELCAST_ENTERPRISE_PASSWORD) &&
       connectionParameters.containsKey(HAZELCAST_CLUSTER_NAME)

   fun isCloudConfig() = connectionParameters.containsKey(HazelcastConnection.VIRIDIAN_CLUSTER_ID)
      && connectionParameters.containsKey(VIRIDIAN_DISCOVERY_TOKEN)
      && connectionParameters.containsKey(HazelcastConnection.VIRIDIAN_SSL_ENABLED)

   fun isSslEnabledCloudConfig() = connectionParameters[HazelcastConnection.VIRIDIAN_SSL_ENABLED]?.toBoolean() == true

   fun hazelcastCloudSslConfiguration(): Properties {
      val props = Properties()
      props.apply {
         setProperty("javax.net.ssl.keyStore", connectionParameters[HazelcastConnection.VIRIDIAN_KEY_STORE_FILE_PATH])
         setProperty("javax.net.ssl.keyStorePassword", connectionParameters[VIRIDIAN_KEY_STORE_PASSWORD])
         setProperty("javax.net.ssl.trustStore", connectionParameters[HazelcastConnection.VIRIDIAN_TRUST_STORE_FILE_PATH])
         setProperty("javax.net.ssl.trustStorePassword", connectionParameters[VIRIDIAN_TRUST_STORE_PASSWORD])
      }
      return props
   }

   fun hazelcastCloudDiscoveryToken() = connectionParameters[VIRIDIAN_DISCOVERY_TOKEN]
   fun hazelcastCloudClusterName() = connectionParameters[HazelcastConnection.VIRIDIAN_CLUSTER_ID]

   fun hazelcastClusterName() = connectionParameters[HAZELCAST_CLUSTER_NAME]

   fun hazelcastClientName() = connectionParameters[HAZELCAST_CLIENT_NAME]

   fun username() = connectionParameters[HAZELCAST_ENTERPRISE_USERNAME];
   fun password() = connectionParameters[HAZELCAST_ENTERPRISE_PASSWORD];
}

object HazelcastConnection {
   const val DRIVER_NAME = "hazelcast"
   const val VIRIDIAN_CLUSTER_ID = "clusterId"
   const val VIRIDIAN_DISCOVERY_TOKEN = "discoveryToken"
   const val VIRIDIAN_KEY_STORE_PASSWORD = "keystorePassword"
   const val VIRIDIAN_SSL_ENABLED = "sslEnabled"
   const val VIRIDIAN_KEY_STORE_FILE_PATH = "keyStorePath"
   const val VIRIDIAN_TRUST_STORE_FILE_PATH = "trustStorePath"
   const val VIRIDIAN_TRUST_STORE_PASSWORD = "truststorePassword"

   const val HAZELCAST_ENTERPRISE_USERNAME = "username"
   const val HAZELCAST_ENTERPRISE_PASSWORD = "password"
   const val HAZELCAST_CLUSTER_NAME = "clusterName"
   const val HAZELCAST_CLIENT_NAME = "clientName"
}
