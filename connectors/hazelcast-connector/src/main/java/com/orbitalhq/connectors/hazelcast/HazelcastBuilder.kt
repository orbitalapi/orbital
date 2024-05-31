package com.orbitalhq.connectors.hazelcast

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.config.InMemoryFormat
import com.hazelcast.config.NearCacheConfig
import com.hazelcast.config.SSLConfig
import com.hazelcast.config.SerializerConfig
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import com.orbitalhq.schema.consumer.SchemaStore

object HazelcastBuilder {
   fun build(config: HazelcastConfiguration, instanceNameSuffix: String = "", schemaStore: SchemaStore): HazelcastInstance {
      val clientConfig = ClientConfig().apply {
         config.hazelcastClusterName()?.let {
            clusterName = it
         }

//         addNearCacheConfig(NearCacheConfig().apply {
//            name = HazelcastMapCachingProvider.OPERATION_CACHE_NAME
//            inMemoryFormat = InMemoryFormat.OBJECT
//            setInvalidateOnChange(true)
//            timeToLiveSeconds = 3000
//            setCacheLocalEntries(true)
//            setMaxIdleSeconds(120)
//         })

         serializationConfig.addSerializerConfig(SerializerConfig().apply {
            implementation = ExpiringByteArrayCustomSerializer()
            typeClass = ExpiringByteArray::class.java
         })

         serializationConfig.addSerializerConfig(SerializerConfig().apply {
            implementation = ExpiringTypedInstanceCustomSerializer(schemaStore)
            typeClass = ExpiringTypedInstance::class.java
         })
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
