package io.vyne


// Interface hoop-jumping to avoid a spring dependency in Vyne
data class DefaultVyneCacheConfiguration(
   override val vyneGraphBuilderCache: VyneGraphBuilderCacheSettings,
   override val vyneDiscoverGraphQuery: HipsterDiscoverGraphQueryStrategyCacheConfiguration
) : VyneCacheConfiguration

interface VyneCacheConfiguration {
   val vyneGraphBuilderCache: VyneGraphBuilderCacheSettings
   val vyneDiscoverGraphQuery: HipsterDiscoverGraphQueryStrategyCacheConfiguration

   companion object {
      fun default(): VyneCacheConfiguration = DefaultVyneCacheConfiguration(
         VyneGraphBuilderCacheSettings(),
         HipsterDiscoverGraphQueryStrategyCacheConfiguration()
      )
   }
}

data class VyneGraphBuilderCacheSettings(
   val baseSchemaCacheSize: Long = 10L,
   val graphWithFactTypesCacheSize: Long = 10L,
   val baseSchemaGraphCacheSize: Long = 10L
)

data class HipsterDiscoverGraphQueryStrategyCacheConfiguration(
   val schemaGraphCacheSize: Long = 5L,
   val searchPathExclusionsCacheSize: Int = 300000
)
