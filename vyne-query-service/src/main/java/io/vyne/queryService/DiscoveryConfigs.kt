package io.vyne.queryService

import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Configuration

@EnableEurekaClient
@Configuration
class ExternalDiscoveryServerConfig {
   @Autowired
   fun logInfo() {
      log().info("Using external service discovery")
   }
}
