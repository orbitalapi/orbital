package io.vyne.schemaServer

import io.vyne.schemaServer.file.FileSchemaConfig
import io.vyne.schemaServer.file.FileSystemSchemaRepository
import io.vyne.schemaServer.git.GitRepository
import io.vyne.schemaServer.git.GitSchemaConfig
import io.vyne.schemaServer.openapi.OpenApiServicesConfig
import io.vyne.schemaServer.openapi.OpenApiVersionedSourceLoader
import io.vyne.schemaServer.publisher.SourceWatchingSchemaPublisher
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.spring.EnableVyneSchemaStore
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

private val logger = KotlinLogging.logger {}

@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableEurekaClient
@EnableConfigurationProperties(
   value = [
      GitSchemaConfig::class,
      OpenApiServicesConfig::class,
      FileSchemaConfig::class,
      VyneSpringHazelcastConfiguration::class
   ]
)
@EnableVyneSchemaStore
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


@Configuration
class SchemaPublicationConfig {
   @Bean
   fun fileSchemaChangePublisher(
      openApiVersionedSourceLoaders: List<OpenApiVersionedSourceLoader>,
      gitRepositories: List<GitRepository>,
      fileRepositories: List<FileSystemSchemaRepository>,
      schemaPublisher: SchemaPublisher
   ): SourceWatchingSchemaPublisher {
      val loaders: List<VersionedSourceLoader> = openApiVersionedSourceLoaders + gitRepositories + fileRepositories
      return SourceWatchingSchemaPublisher(loaders,schemaPublisher)
   }
}
