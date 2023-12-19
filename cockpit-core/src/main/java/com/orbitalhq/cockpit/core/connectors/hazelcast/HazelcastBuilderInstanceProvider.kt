package com.orbitalhq.cockpit.core.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import com.orbitalhq.connectors.hazelcast.HazelcastBuilder
import com.orbitalhq.connectors.hazelcast.HazelcastInstanceProvider
import org.springframework.stereotype.Component

@Component
class HazelcastBuilderInstanceProvider: HazelcastInstanceProvider {
   override fun provide(config: HazelcastConfiguration): HazelcastInstance {
      return HazelcastBuilder.build(config)
   }
}
