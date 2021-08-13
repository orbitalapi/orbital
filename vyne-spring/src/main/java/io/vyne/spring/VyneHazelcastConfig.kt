package io.vyne.spring

import com.hazelcast.config.Config
import com.hazelcast.config.DiscoveryStrategyConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.instance.AddressPicker
import com.hazelcast.instance.DefaultNodeContext
import com.hazelcast.instance.HazelcastInstanceFactory
import com.hazelcast.instance.Node
import com.hazelcast.logging.Slf4jFactory
import org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryConfiguration
import org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryStrategyFactory
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class VyneHazelcastConfig {

   @Bean("hazelcast")
   @ConditionalOnProperty(VYNE_SCHEMA_PUBLICATION_METHOD, havingValue = "DISTRIBUTED")
   @Profile("!swarm")
   fun defaultHazelCastInstance(): HazelcastInstance {
      return Hazelcast.newHazelcastInstance()
   }

   @Bean("hazelcast")
   @ConditionalOnProperty(VYNE_SCHEMA_PUBLICATION_METHOD, havingValue = "DISTRIBUTED")
   @Profile("swarm")
   fun swarmHazelCastInstance(): HazelcastInstance {
      val dockerNetworkName = System.getenv("DOCKER_NETWORK_NAME") ?: System.getProperty(SwarmAddressPicker.PROP_DOCKER_NETWORK_NAMES)
      val dockerServiceName = System.getenv("DOCKER_SERVICE_NAME") ?: System.getProperty(SwarmAddressPicker.PROP_DOCKER_SERVICE_NAMES)
      val dockerServiceLabel = System.getenv("DOCKER_SERVICE_LABELS") ?: System.getProperty(SwarmAddressPicker.PROP_DOCKER_SERVICE_LABELS)
      val hazelcastPeerPortString = System.getenv("HAZELCAST_PEER_PORT") ?: System.getProperty(SwarmAddressPicker.PROP_HAZELCAST_PEER_PORT)
      val hazelcastPeerPort = hazelcastPeerPortString?.let { it.toInt() } ?: 5701
      val swarmedConfig = Config().apply {
         networkConfig.join.multicastConfig.isEnabled = false
         networkConfig.memberAddressProviderConfig.isEnabled = true
         networkConfig.join.discoveryConfig.addDiscoveryStrategyConfig(
            DiscoveryStrategyConfig(
               DockerSwarmDiscoveryStrategyFactory(), mapOf(
               DockerSwarmDiscoveryConfiguration.DOCKER_NETWORK_NAMES.key() to dockerNetworkName,
               DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_NAMES.key() to dockerServiceName,
               DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_LABELS.key() to dockerServiceLabel
            ).filterValues { it != null })
         )
      }
      HazelcastInstanceFactory.newHazelcastInstance(swarmedConfig, null, object : DefaultNodeContext() {
         override fun createAddressPicker(node: Node): AddressPicker {
            return SwarmAddressPicker(
               Slf4jFactory().getLogger("SwarmAddressPicker"),
               dockerNetworkName,
               dockerServiceName,
               dockerServiceLabel,
               hazelcastPeerPort
            )
         }
      })
      return Hazelcast.newHazelcastInstance(swarmedConfig)
   }
}
