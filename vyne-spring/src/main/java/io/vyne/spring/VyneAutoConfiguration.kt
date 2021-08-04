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
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.utils.log
import org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryConfiguration.DOCKER_NETWORK_NAMES
import org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_LABELS
import org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_NAMES
import org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryStrategyFactory
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker.PROP_DOCKER_NETWORK_NAMES
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker.PROP_DOCKER_SERVICE_LABELS
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker.PROP_DOCKER_SERVICE_NAMES
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker.PROP_HAZELCAST_PEER_PORT
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.util.*

const val VYNE_SCHEMA_PUBLICATION_METHOD = "vyne.schema.publicationMethod"

@Configuration
@AutoConfigureAfter(VyneSchemaConsumerConfigRegistrar::class, RibbonAutoConfiguration::class)
// TODO : re-enbale external schema services.
// https://gitlab.com/vyne/vyne/issues/14
//@EnableFeignClients(basePackageClasses = arrayOf(SchemaService::class))
// Don't enable Vyne if we're configuring to be a Schema Discovery service
@ConditionalOnMissingClass("io.vyne.schemaStore.TaxiSchemaService")

// If someone is only running a VyneClient,(ie @EnableVyneClient) they don't want the stuff inside this config
// If they've @EnableVynePublisher, then a LocalTaxiSchemaProvider will have been configured.
//
// MP 4-Aug-21:  Removing this ConditionalOnBean to see what breaks.
// @EnableVyneClient and @EnableVyne are hopefully sufficiently decoupled now that
// we don't need this check.
// If something is broken, document here.
//@ConditionalOnBean(LocalTaxiSchemaProvider::class)
class VyneAutoConfiguration {

   @Bean
   @Primary
   fun schemaProvider(
      localTaxiSchemaProvider: Optional<LocalTaxiSchemaProvider>,
      remoteTaxiSchemaProvider: Optional<RemoteTaxiSourceProvider>
   ): SchemaSourceProvider {
      return when {
         remoteTaxiSchemaProvider.isPresent -> remoteTaxiSchemaProvider.get()
         localTaxiSchemaProvider.isPresent -> localTaxiSchemaProvider.get()
         else -> {
            log().warn("No schema provider (either local or remote).  Using an empty schema provider")
            SimpleTaxiSchemaProvider("")
         }
      }
   }

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
      val dockerNetworkName = System.getenv("DOCKER_NETWORK_NAME") ?: System.getProperty(PROP_DOCKER_NETWORK_NAMES)
      val dockerServiceName = System.getenv("DOCKER_SERVICE_NAME") ?: System.getProperty(PROP_DOCKER_SERVICE_NAMES)
      val dockerServiceLabel = System.getenv("DOCKER_SERVICE_LABELS") ?: System.getProperty(PROP_DOCKER_SERVICE_LABELS)
      val hazelcastPeerPortString = System.getenv("HAZELCAST_PEER_PORT") ?: System.getProperty(PROP_HAZELCAST_PEER_PORT)
      val hazelcastPeerPort = hazelcastPeerPortString?.let { it.toInt() } ?: 5701
      val swarmedConfig = Config().apply {
         networkConfig.join.multicastConfig.isEnabled = false
         networkConfig.memberAddressProviderConfig.isEnabled = true
         networkConfig.join.discoveryConfig.addDiscoveryStrategyConfig(
            DiscoveryStrategyConfig(DockerSwarmDiscoveryStrategyFactory(), mapOf(
               DOCKER_NETWORK_NAMES.key() to dockerNetworkName,
               DOCKER_SERVICE_NAMES.key() to dockerServiceName,
               DOCKER_SERVICE_LABELS.key() to dockerServiceLabel
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

fun BeanDefinitionRegistry.registerBeanDefinitionOfType(clazz: Class<*>): String {
   val beanName = clazz.simpleName
   this.registerBeanDefinition(
      beanName,
      BeanDefinitionBuilder.genericBeanDefinition(clazz).beanDefinition
   )
   return beanName
}
