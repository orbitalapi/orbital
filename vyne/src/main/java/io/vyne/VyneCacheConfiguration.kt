package io.vyne

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.graph")
data class VyneCacheConfiguration(
   val vyneGraphBuilderCache: VyneGraphBuilderCacheSettings,
   val vyneDiscoverGraphQuery: HipsterDiscoverGraphQueryStrategyCacheConfiguration) {
   companion object {
      fun default() = VyneCacheConfiguration(
         VyneGraphBuilderCacheSettings(100L, 100L, 100L),
         HipsterDiscoverGraphQueryStrategyCacheConfiguration(5L, 300000)
      )
   }
}

data class VyneGraphBuilderCacheSettings(val baseSchemaCacheSize: Long, val graphWithFactTypesCacheSize: Long, val baseSchemaGraphCacheSize: Long)
data class HipsterDiscoverGraphQueryStrategyCacheConfiguration(val schemaGraphCacheSize: Long, val searchPathExclusionsCacheSize: Int)
