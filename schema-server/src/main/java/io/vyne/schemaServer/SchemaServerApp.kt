package io.vyne.schemaServer

import io.vyne.schemaPublisherApi.SchemaPublisher
import io.vyne.schemaServer.core.VersionedSourceLoader
import io.vyne.schemaServer.core.file.FileSystemSchemaRepository
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitRepositorySourceLoader
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
import io.vyne.schemaServer.core.openApi.OpenApiSchemaRepositoryConfig
import io.vyne.schemaServer.core.openApi.OpenApiVersionedSourceLoader
import io.vyne.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import io.vyne.spring.EnableVyneSchemaStore
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@EnableAsync
@SpringBootApplication(scanBasePackageClasses = [SchemaServerApp::class, VersionedSourceLoader::class])
@EnableScheduling
@EnableEurekaClient
@EnableConfigurationProperties(
   value = [VyneSpringHazelcastConfiguration::class]
)
@EnableVyneSchemaStore
class SchemaServerApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(SchemaServerApp::class.java, *args)
      }
   }

   // Place this here to allow overriding of the config loader in tests.
   @Bean
   fun configRepoLoader(@Value("\${vyne.repositories.config-file:repositories.conf}") configFilePath: Path,
                        @Value("\${vyne.repositories.repository-path:#{null}}") repositoryHome: Path? = null,
                        ): io.vyne.schemaServer.core.SchemaRepositoryConfigLoader {
      return if (repositoryHome != null) {
         logger.info { "vyne.repositories.repository-path was set to $repositoryHome running a file-based repository from this path, ignoring any other config from $configFilePath" }
         return io.vyne.schemaServer.core.InMemorySchemaRepositoryConfigLoader(
            io.vyne.schemaServer.core.SchemaRepositoryConfig(
               FileSystemSchemaRepositoryConfig(
                  paths = listOf(repositoryHome)
               )
            )
         )
      } else {
         logger.info { "Using repository config file at $configFilePath" }
         io.vyne.schemaServer.core.FileSchemaRepositoryConfigLoader(configFilePath)
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

   // TODO : We will eventually need to defer all this stuff once we allow changing
   // repo config at runtime (ie., via a rest service)

   @Bean
   fun gitConfig(loader: io.vyne.schemaServer.core.SchemaRepositoryConfigLoader): GitSchemaRepositoryConfig {
      return loader.load().git ?: GitSchemaRepositoryConfig()
   }

   @Bean
   fun openApiConfig(loader: io.vyne.schemaServer.core.SchemaRepositoryConfigLoader): OpenApiSchemaRepositoryConfig {
      return loader.load().openApi ?: OpenApiSchemaRepositoryConfig()
   }

   @Bean
   fun fileConfig(loader: io.vyne.schemaServer.core.SchemaRepositoryConfigLoader): FileSystemSchemaRepositoryConfig {
      return loader.load().file ?: FileSystemSchemaRepositoryConfig()
   }

   @Bean
   fun fileSchemaChangePublisher(
      openApiVersionedSourceLoaders: List<OpenApiVersionedSourceLoader>,
      gitRepositories: List<GitRepositorySourceLoader>,
      fileRepositories: List<FileSystemSchemaRepository>,
      schemaPublisher: SchemaPublisher
   ): SourceWatchingSchemaPublisher {
      val loaders: List<io.vyne.schemaServer.core.VersionedSourceLoader> = openApiVersionedSourceLoaders + gitRepositories + fileRepositories
      logger.info {"Detected ${loaders.size} total loaders - ${openApiVersionedSourceLoaders.size} openApi loaders, ${gitRepositories.size} gitRepository loaders, ${fileRepositories.size} fileRepository loaders"  }
      return SourceWatchingSchemaPublisher(loaders, schemaPublisher)
   }
}

