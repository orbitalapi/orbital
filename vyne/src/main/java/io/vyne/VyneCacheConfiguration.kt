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
         VyneGraphBuilderCacheSettings(),
         HipsterDiscoverGraphQueryStrategyCacheConfiguration()
      )
   }
}

data class VyneGraphBuilderCacheSettings(val baseSchemaCacheSize: Long = 100L, val graphWithFactTypesCacheSize: Long = 100L, val baseSchemaGraphCacheSize: Long = 100L)
data class HipsterDiscoverGraphQueryStrategyCacheConfiguration(val schemaGraphCacheSize: Long = 5L, val searchPathExclusionsCacheSize: Int = 300000)
