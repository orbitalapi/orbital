package io.vyne.schemaServer

import io.vyne.schemaServer.git.GitSchemaRepoConfig
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableEurekaClient
@EnableConfigurationProperties(GitSchemaRepoConfig::class, VyneSpringHazelcastConfiguration::class)
@VyneSchemaPublisher
class SchemaServerApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(SchemaServerApp::class.java, *args)
      }
   }
}