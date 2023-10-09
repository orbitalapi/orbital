package com.orbitalhq.connectors.hazelcast

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration

object HazelcastBuilder {
   fun build(config:HazelcastConfiguration):HazelcastInstance {
      val clientConfig = ClientConfig().apply {
         networkConfig.addAddress(*config.addresses.toTypedArray())
      }
      return HazelcastClient.newHazelcastClient(clientConfig)
   }
}
