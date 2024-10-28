package com.orbitalhq.connectors.hazelcast

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.client.config.XmlClientConfigBuilder
import com.hazelcast.client.config.YamlClientConfigBuilder
import com.hazelcast.config.SSLConfig
import com.hazelcast.config.SerializationConfig
import com.hazelcast.config.SerializerConfig
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration

object HazelcastBuilder {

   fun customSerializers(): List<SerializerConfig> {
      return listOf(
         SerializerConfig().apply {
            implementation = ExpiringTypedInstanceCustomSerializer()
            typeClass = CachedTypedInstanceList::class.java
         },
      )
   }

   fun serializationConfig(): SerializationConfig {
      return SerializationConfig().apply {
         customSerializers().forEach {
            addSerializerConfig(it)
         }
         compactSerializationConfig
            .addSerializer(QualifiedNameCompactSerializer)
            .addSerializer(OperationParamCompactSerializer)
      }
   }


   fun build(
      config: HazelcastConfiguration,
      instanceNameSuffix: String = ""
   ): HazelcastInstance {
     return  when {
         config.xmlConfig != null ->  {
           val xmlConfig =  XmlClientConfigBuilder(config.xmlConfigFilePath())
            HazelcastClient.newHazelcastClient(xmlConfig.build().apply {
               serializationConfig = serializationConfig()
            })
         }

         config.yamlConfig != null -> {
            val yamlConfig = YamlClientConfigBuilder(config.yamlConfigFilePath())
            HazelcastClient.newHazelcastClient(yamlConfig.build().apply {
               serializationConfig = serializationConfig()
            })
         }

         else -> {
            val clientConfig = ClientConfig().apply {
               config.hazelcastClusterName()?.let {
                  clusterName = it
               }
               serializationConfig = serializationConfig()

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
            HazelcastClient.newHazelcastClient(clientConfig)
         }
      }

   }
}

interface HazelcastInstanceProvider {
   fun provide(config: HazelcastConfiguration): HazelcastInstance

   /**
    * Returns the hazelcast connection for the provided name.
    * If the name is null, and a default connection has been configured, then
    * the default is returned - otherwise an exception is thrown
    */
   fun hazelcastConnection(connectionName: String?): Pair<HazelcastInstance, HazelcastConfiguration>

   /**
    * Indicates if the provider has an instance for the specified name.
    * If no name is provided, indicates if a default connection has been
    * specified
    */
   fun canProvideHazelcastInstance(connectionName: String?): Boolean
}

fun HazelcastInstance.doHealthCheck() {
   executeTransaction { _ ->
      localEndpoint.uuid.toString()
   }
}
