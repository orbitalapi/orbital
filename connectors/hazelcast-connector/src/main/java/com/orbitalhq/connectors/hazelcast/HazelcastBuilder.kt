package com.orbitalhq.connectors.hazelcast

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.config.SSLConfig
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration

object HazelcastBuilder {
   fun build(config: HazelcastConfiguration, instanceNameSuffix: String = ""): HazelcastInstance {
      val clientConfig = ClientConfig().apply {
         config.hazelcastClusterName()?.let {
            clusterName = it
         }

         config.hazelcastClientName()?.let {
            instanceName = "${it}$instanceNameSuffix"
         }
         when {
            config.isSslEnabledCloudConfig() -> {
               networkConfig.sslConfig = SSLConfig().apply {
                  isEnabled = true
                  properties = config.hazelcastCloudSslConfiguration()
               }

               networkConfig.cloudConfig.apply {
                  discoveryToken = config.hazelcastCloudDiscoveryToken()
                  isEnabled = true
                  clusterName = config.hazelcastCloudClusterName()
               }
            }

            config.isCloudConfig() -> {
               networkConfig.cloudConfig.apply {
                  discoveryToken = config.hazelcastCloudDiscoveryToken()
                  isEnabled = true
                  clusterName = config.hazelcastCloudClusterName()
               }
            }

            config.userNamePasswordAuthentication() -> {
               securityConfig.setUsernamePasswordIdentityConfig(config.username()!!, config.password()!!)
               networkConfig.addAddress(*config.addresses.toTypedArray())
            }

            else -> {
               networkConfig.addAddress(*config.addresses.toTypedArray())
            }
         }

      }
      return HazelcastClient.newHazelcastClient(clientConfig)
   }
}

interface HazelcastInstanceProvider {
   fun provide(config: HazelcastConfiguration): HazelcastInstance
}

fun HazelcastInstance.doHealthCheck() {
   executeTransaction { _ ->
      localEndpoint.uuid.toString()
   }
}
