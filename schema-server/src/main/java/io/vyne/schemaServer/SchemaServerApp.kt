package io.vyne.schemaServer

import io.vyne.schemaServer.git.GitSchemaRepoConfig
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.utils.log
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

private val logger = KotlinLogging.logger {}

@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableEurekaClient
@EnableConfigurationProperties(GitSchemaRepoConfig::class)
@VyneSchemaPublisher
class SchemaServerApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(SchemaServerApp::class.java, *args)
      }
   }

   @Autowired
   fun logInfo(@Autowired(required = false) buildInfo: BuildProperties? = null) {
      val baseVersion = buildInfo?.get("baseVersion")
      val buildNumber = buildInfo?.get("buildNumber")
      val version = if (!baseVersion.isNullOrEmpty() && buildNumber != "0" && buildInfo.version.contains("SNAPSHOT")) {
         "$baseVersion-BETA-$buildNumber"
      } else {
         buildInfo?.version ?: "Dev version"
      }

      logger.info { "Schema server version => $version" }
   }
}


