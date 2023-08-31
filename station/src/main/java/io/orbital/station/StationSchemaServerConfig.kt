package io.orbital.station

import io.vyne.schemaServer.core.SchemaServerConfig
import io.vyne.schemaServer.core.config.WorkspaceLoaderConfig
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@ComponentScan(
   basePackageClasses = [SchemaServerConfig::class]
)
@Configuration
@EnableAsync
@EnableScheduling
@EnableDiscoveryClient
@EnableConfigurationProperties(
   value = [VyneSpringHazelcastConfiguration::class]
)
@Import(WorkspaceLoaderConfig::class)
class StationSchemaServerConfig {


}

