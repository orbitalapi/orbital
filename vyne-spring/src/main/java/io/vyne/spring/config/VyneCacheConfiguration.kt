package io.vyne.spring.config

import io.vyne.HipsterDiscoverGraphQueryStrategyCacheConfiguration
import io.vyne.VyneCacheConfiguration
import io.vyne.VyneGraphBuilderCacheSettings
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Spring annotated version of VyneCacheConfiguration.
 * Lives in a seperate package to avoid spring dependencies in Vyne
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.graph")
data class VyneSpringCacheConfiguration(
   override val vyneGraphBuilderCache: VyneGraphBuilderCacheSettings,
   override val vyneDiscoverGraphQuery: HipsterDiscoverGraphQueryStrategyCacheConfiguration
) : VyneCacheConfiguration
