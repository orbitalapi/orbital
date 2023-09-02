package io.orbital.station

import com.orbitalhq.history.AnalyticsConfig
import com.orbitalhq.query.runtime.core.EnableVyneQueryNode
import com.orbitalhq.query.runtime.core.QueryNodeConfig
import com.orbitalhq.spring.EnableVyne
import com.orbitalhq.spring.VyneSchemaConsumer
import com.orbitalhq.spring.config.DiscoveryClientConfig
import com.orbitalhq.spring.config.VyneSpringCacheConfiguration
import com.orbitalhq.spring.config.VyneSpringProjectionConfiguration
import com.orbitalhq.spring.http.auth.HttpAuthConfig
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
