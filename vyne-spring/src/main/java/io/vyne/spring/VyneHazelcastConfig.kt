package io.vyne.spring

import com.hazelcast.config.Config
import com.hazelcast.config.ExecutorConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.eureka.one.EurekaOneDiscoveryStrategyFactory
import com.netflix.discovery.EurekaClient
import io.vyne.schemas.DistributedSchemaConfig.vyneSchemaMapConfig
import io.vyne.spring.config.HazelcastDiscovery
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import io.vyne.spring.projection.VyneHazelcastMemberTags
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VyneHazelcastConfig(
   val vyneHazelcastConfiguration: VyneSpringHazelcastConfiguration,
   val eurekaClient: EurekaClient?
) {

   @Bean("hazelcast")
   @ConditionalOnProperty("vyne.projection.distributionMode", havingValue = "DISTRIBUTED")
   fun vyneHazelcastInstance(): HazelcastInstance {

      val hazelcastConfiguration = Config()
      hazelcastConfiguration.addMapConfig(vyneSchemaMapConfig())
      hazelcastConfiguration.executorConfigs["projectionExecutorService"] = projectionExecutorServiceConfig()

      EurekaOneDiscoveryStrategyFactory.setEurekaClient(eurekaClient)

      when (vyneHazelcastConfiguration.discovery) {
         HazelcastDiscovery.MULTICAST -> hazelcastConfiguration.apply { multicastHazelcastConfig(this) }
         HazelcastDiscovery.AWS -> hazelcastConfiguration.apply { awsHazelcastConfig(this) }
         HazelcastDiscovery.EUREKA -> {
            hazelcastConfiguration.apply { eurekaHazelcastConfig(this, vyneHazelcastConfiguration.eurekaUri) }
         }
      }

      val instance = Hazelcast.newHazelcastInstance(hazelcastConfiguration)
      instance.cluster.localMember.attributes[VyneHazelcastMemberTags.VYNE_TAG.tag] = VyneHazelcastMemberTags.QUERY_SERVICE_TAG.tag

      return instance

   }


   private fun awsHazelcastConfig(config:Config): Config {

      val AWS_REGION = System.getenv("AWS_REGION") ?: System.getProperty("AWS_REGION")

      config.executorConfigs["projectionExecutorService"] = projectionExecutorServiceConfig()
      config.networkConfig.join.multicastConfig.isEnabled = false
      config.networkConfig.join.awsConfig.isEnabled = true
      config.networkConfig.join.awsConfig
         .setProperty("hz-port", vyneHazelcastConfiguration.awsPortScanRange)
         .setProperty("region", AWS_REGION)

      if (vyneHazelcastConfiguration.networkInterface.isNotEmpty()) {
         config.setProperty("hazelcast.socket.bind.any", "false")
         config.networkConfig.interfaces.isEnabled = true
         config.networkConfig.interfaces.interfaces = listOf(vyneHazelcastConfiguration.networkInterface)
      }

      return config
   }

   fun multicastHazelcastConfig(config:Config): Config {
      config.networkConfig.join.multicastConfig.isEnabled = true
      if (vyneHazelcastConfiguration.networkInterface.isNotEmpty()) {
         config.setProperty("hazelcast.socket.bind.any", "false")
         config.networkConfig.interfaces.isEnabled = true
         config.networkConfig.interfaces.interfaces = listOf(vyneHazelcastConfiguration.networkInterface)
      }
      return config
   }

   fun eurekaHazelcastConfig(config:Config, eurekaUri: String): Config {

      config.apply {

         networkConfig.join.tcpIpConfig.isEnabled = false
         networkConfig.join.multicastConfig.isEnabled = false
         networkConfig.join.eurekaConfig.isEnabled = true
         networkConfig.join.eurekaConfig.setProperty("self-registration", "true")
         networkConfig.join.eurekaConfig.setProperty("namespace", "hazelcast")
         networkConfig.join.eurekaConfig.setProperty("use-metadata-for-host-and-port", vyneHazelcastConfiguration.useMetadataForHostAndPort)
         networkConfig.join.eurekaConfig.setProperty("use-classpath-eureka-client-props", "false")
         networkConfig.join.eurekaConfig.setProperty("shouldUseDns", "false")
         networkConfig.join.eurekaConfig.setProperty("serviceUrl.default", eurekaUri)

         if (vyneHazelcastConfiguration.networkInterface.isNotEmpty()) {
            config.setProperty("hazelcast.socket.bind.any", "false")
            networkConfig.interfaces.isEnabled = true
            networkConfig.interfaces.interfaces = listOf(vyneHazelcastConfiguration.networkInterface)
         }
      }

      return config

   }

   fun projectionExecutorServiceConfig():ExecutorConfig {

      val projectionExecutorServiceConfig = ExecutorConfig()
      projectionExecutorServiceConfig.poolSize = vyneHazelcastConfiguration.taskPoolSize
      projectionExecutorServiceConfig.queueCapacity = vyneHazelcastConfiguration.taskQueueSize
      projectionExecutorServiceConfig.isStatisticsEnabled = true
      return projectionExecutorServiceConfig
   }
}
