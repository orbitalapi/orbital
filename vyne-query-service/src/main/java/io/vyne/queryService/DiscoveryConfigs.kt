package io.vyne.queryService

import io.vyne.utils.log
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@ConstructorBinding
@ConfigurationProperties("vyne.service-discovery")
data class VyneDiscoveryClientConfig(
   val client: DiscoveryClient
) {
   enum class DiscoveryClient {
      EUREKA,
      SIMPLE;
      // TODO : Support Consul
   }
}

@EnableDiscoveryClient(autoRegister = false)
@Configuration
class ExternalDiscoveryServerConfig {
}


@ConditionalOnProperty(value = ["vyne.service-discovery.client"], havingValue = "EUREKA")
@Configuration()
@Import(
   EurekaClientAutoConfiguration::class,
)
@EnableEurekaClient
class EurekaDiscoveryClientConfig {
   private val logger = KotlinLogging.logger {}
   @Autowired
   fun logInfo() {
      logger.info("Using Eureka for service discovery")
   }
}

@ConditionalOnProperty(value = ["vyne.service-discovery.client"], havingValue = "SIMPLE")
@Configuration
@Import(
   SimpleDiscoveryClientAutoConfiguration::class
)
class SimpleDiscoveryClientConfig {
   @Autowired
   fun logInfo(discoveryClient: DiscoveryClient) {
      log().info("Using a simple property-based Discovery Client for service discovery")
   }
//
//   @Autowired(required = false)
//   private var server: ServerProperties? = null
//
//   @Value("\${spring.application.name:application}")
//   private lateinit var serviceId: String
//
//   @Autowired
//   private lateinit var inet: InetUtils
//
//   private var port = 0
//
//   private val simple = SimpleDiscoveryProperties()
//
//   @Bean
//   fun simpleDiscoveryProperties(): SimpleDiscoveryProperties {
//      simple.local.serviceId = serviceId
//      simple.local.uri = URI.create(
//         "http://" + inet.findFirstNonLoopbackHostInfo().hostname
//            + ":" + findPort()
//      )
//      return simple
//   }
//
//   @Bean
//   fun simpleDiscoveryClient(properties: SimpleDiscoveryProperties): DiscoveryClient {
//      return SimpleDiscoveryClient(properties)
//   }
//
//   private fun findPort(): Int {
//      if (port > 0) {
//         return port
//      }
//      return if ((server?.port ?: 0) > 0) {
//         server!!.port
//      } else 8080
//   }
//
//   fun onApplicationEvent(webServerInitializedEvent: WebServerInitializedEvent) {
//      port = webServerInitializedEvent.webServer.port
//      if (port > 0) {
//         simple.local.uri = URI.create(
//            "http://"
//               + inet!!.findFirstNonLoopbackHostInfo().hostname + ":"
//               + port
//         )
//      }
//   }
}
