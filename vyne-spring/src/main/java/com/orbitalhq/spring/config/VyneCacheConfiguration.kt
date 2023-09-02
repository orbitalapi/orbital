package com.orbitalhq.spring.config

import com.orbitalhq.HipsterDiscoverGraphQueryStrategyCacheConfiguration
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.VyneGraphBuilderCacheSettings
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
