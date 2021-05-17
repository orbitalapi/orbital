package io.vyne.dataQuality.hub

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableEurekaClient
class DataQualityHubApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(DataQualityHubApp::class.java)
         app.run(*args)
      }
   }
}


@Configurable
@EnableJpaRepositories
class RepositoryConfig
