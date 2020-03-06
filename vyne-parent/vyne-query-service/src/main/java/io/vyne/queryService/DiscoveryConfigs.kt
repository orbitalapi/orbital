package io.vyne.queryService

import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.annotation.PostConstruct

@Profile("embedded-discovery")
@EnableEurekaServer
@Configuration
class EmbeddedDiscoveryServerConfig {
   @Value("\${eureka.dashboard.path}")
   lateinit var eurekaDashboardPath: String

   @Value("\${server.port}")
   lateinit var serverPort: String

   @PostConstruct
   fun logInfo() {
      log().info("An embedded Eureka instance is running.  Dashboard is available at  http://localhost:$serverPort/$eurekaDashboardPath")
   }
}

@Profile("!embedded-discovery")
@EnableEurekaClient
@Configuration
class ExternalDiscoveryServerConfig {
   @Autowired
   fun logInfo() {
      log().info("Using external service discovery")
   }
}
