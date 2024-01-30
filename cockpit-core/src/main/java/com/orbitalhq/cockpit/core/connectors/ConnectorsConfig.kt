package com.orbitalhq.cockpit.core.connectors

import com.orbitalhq.config.ConfigSourceLoader
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.schema.consumer.ProjectManagerConfigSourceLoader
import com.orbitalhq.schema.consumer.SchemaConfigSourceLoader
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.spring.config.EnvVariablesConfig
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class, EnvVariablesConfig::class)
class ConnectorsConfig {
   @Bean
   fun connectorsConfigRegistry(
      config: VyneConnectionsConfig,
      schemaStore: SchemaStore,
      envVariablesConfig: EnvVariablesConfig,
      additionalLoaders: List<ConfigSourceLoader>?,
      projectManager: ReactiveProjectStoreManager
   ): SourceLoaderConnectorsRegistry {
      val projectManagerConfigSourceLoader = ProjectManagerConfigSourceLoader(
         schemaEventSource = schemaStore,
         projectManager = projectManager,
         filePattern = "connections.conf"
      )
      val builtinLoaders = listOf(
         FileConfigSourceLoader(
            envVariablesConfig.envVariablesPath,
            failIfNotFound = false,
            packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER
         ),
         SchemaConfigSourceLoader(schemaStore, "env.conf"),
         FileConfigSourceLoader(
            config.configFile,
            packageIdentifier = VyneConnectionsConfig.PACKAGE_IDENTIFIER,
            failIfNotFound = false
         ),
         projectManagerConfigSourceLoader
      )

      val totalLoaders = additionalLoaders?.plus(builtinLoaders) ?: builtinLoaders
//      return SourceLoaderConnectorsRegistry(totalLoaders, writerProviders = listOf(projectManager))
      return SourceLoaderConnectorsRegistry(totalLoaders, writerProviders = listOf(projectManagerConfigSourceLoader))
   }
}
