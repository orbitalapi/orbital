package io.vyne.admin

import de.codecentric.boot.admin.server.config.EnableAdminServer
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
@EnableAdminServer
class SpringBootAdminApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplicationBuilder(SpringBootAdminApp::class.java)
            .web(WebApplicationType.REACTIVE)
            .run(*args)
      }
   }
}
