package io.vyne.spring.config

import io.vyne.HipsterDiscoverGraphQueryStrategyCacheConfiguration
import io.vyne.VyneCacheConfiguration
import io.vyne.VyneGraphBuilderCacheSettings
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Spring annotated version of VyneCacheConfiguration.
 * Lives in a separate package to avoid spring dependencies in Vyne
 */
//@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.graph")
data class VyneSpringCacheConfiguration(
   override val vyneGraphBuilderCache: VyneGraphBuilderCacheSettings = VyneGraphBuilderCacheSettings(),
   override val vyneDiscoverGraphQuery: HipsterDiscoverGraphQueryStrategyCacheConfiguration = HipsterDiscoverGraphQueryStrategyCacheConfiguration()
) : VyneCacheConfiguration
