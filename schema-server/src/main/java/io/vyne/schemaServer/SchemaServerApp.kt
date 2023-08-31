package io.vyne.schemaServer

import io.vyne.monitoring.EnableCloudMetrics
import io.vyne.schemaServer.core.VersionedSourceLoader
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.repositories.FileSchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.InMemorySchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfig
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

@EnableAsync
@SpringBootApplication(scanBasePackageClasses = [SchemaServerApp::class, VersionedSourceLoader::class])
@EnableScheduling
// MP: I don't think Schema server needs a discovery client, does it?
//@EnableDiscoveryClient
@EnableConfigurationProperties(
   value = [
      VyneSpringHazelcastConfiguration::class,
      WorkspaceConfig::class
   ]
)
class SchemaServerApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(SchemaServerApp::class.java, *args)
      }
   }

   // Place this here to allow overriding of the config loader in tests.
   @Bean
   fun configRepoLoader(
      workspaceConfig: WorkspaceConfig,
      eventDispatcher: RepositorySpecLifecycleEventDispatcher
   ): SchemaRepositoryConfigLoader {
      return if (workspaceConfig.projectFile != null) {
         logger.info { "A single-project workspace has been configured for ${workspaceConfig.projectFile}. Ignoring any other config from ${workspaceConfig.configFile}" }
         return InMemorySchemaRepositoryConfigLoader(
            SchemaRepositoryConfig(
               FileSystemSchemaRepositoryConfig(
                  projects = listOf(FileSystemPackageSpec(workspaceConfig.projectFile, isEditable = true)),
               )
            ),
            eventDispatcher
         )
      } else {
         logger.info { "Using workspace config file at ${workspaceConfig.configFile}" }
         FileSchemaRepositoryConfigLoader(workspaceConfig.configFile, eventDispatcher = eventDispatcher)
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


@EnableCloudMetrics
class SchemaServerConfig

@ConfigurationProperties(prefix = "vyne.workspace")
data class WorkspaceConfig(
   /**
    * A path to the config file containing the workspace config.
    */
   val configFile: Path = Paths.get("workspace.conf"),

   /**
    * Path to a taxi.conf file.
    *
    * Bootstraps a single-file project, creating a standalone
    * workspace.
    *
    * If this is provided, the workspaceConfigPath is ignored.
    *
    * Useful for quickly bootstrapping demo projects, not intended
    * for production.
    */
   val projectFile: Path? = null
)
