package io.vyne.eureka

import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer

@EnableEurekaServer
@SpringBootApplication
class EurekaApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(EurekaApp::class.java)
         app.run(*args)
      }
   }
}
