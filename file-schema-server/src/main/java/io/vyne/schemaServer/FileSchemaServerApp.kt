package io.vyne.schemaServer

import io.vyne.spring.SchemaPublicationMethod
import io.vyne.spring.VyneSchemaPublisher
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
@EnableConfigurationProperties(GitSchemaRepoConfig::class)
@VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISTRIBUTED)
class FileSchemaServerApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(FileSchemaServerApp::class.java, *args)
      }
   }
}


