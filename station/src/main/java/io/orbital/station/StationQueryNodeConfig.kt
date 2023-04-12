package io.orbital.station

import io.vyne.history.AnalyticsConfig
import io.vyne.query.runtime.core.EnableVyneQueryNode
import io.vyne.query.runtime.core.QueryNodeConfig
import io.vyne.spring.EnableVyne
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.config.DiscoveryClientConfig
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.auth.HttpAuthConfig
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan(basePackageClasses = [QueryNodeConfig::class])
@EnableVyne
@VyneSchemaConsumer
@EnableVyneQueryNode
@Import(
   HttpAuthConfig::class,
   DiscoveryClientConfig::class,
   AnalyticsConfig::class
)
@EnableConfigurationProperties(
   VyneSpringCacheConfiguration::class,
   VyneSpringProjectionConfiguration::class,
)

class StationQueryNodeConfig {
}
