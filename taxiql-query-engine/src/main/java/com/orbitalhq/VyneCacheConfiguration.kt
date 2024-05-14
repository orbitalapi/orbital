package com.orbitalhq

import java.time.Duration


// Interface hoop-jumping to avoid a spring dependency in Vyne
data class DefaultVyneCacheConfiguration(
   override val vyneGraphBuilderCache: VyneGraphBuilderCacheSettings,
   override val vyneDiscoverGraphQuery: HipsterDiscoverGraphQueryStrategyCacheConfiguration,
   override val operationCache: LocalOperationCacheConfiguration
) : VyneCacheConfiguration

interface VyneCacheConfiguration {
   val vyneGraphBuilderCache: VyneGraphBuilderCacheSettings
   val vyneDiscoverGraphQuery: HipsterDiscoverGraphQueryStrategyCacheConfiguration
   val operationCache:LocalOperationCacheConfiguration

   companion object {
      fun default(): VyneCacheConfiguration = DefaultVyneCacheConfiguration(
         VyneGraphBuilderCacheSettings(),
         HipsterDiscoverGraphQueryStrategyCacheConfiguration(),
         LocalOperationCacheConfiguration()
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
   val searchPathExclusionsCacheSize: Int = 300000,
   val invocationCacheSize: Long = 5L
)

/**
 * Config settings for the cache that stores remote call invocations.
 */
data class LocalOperationCacheConfiguration(
   /**
    * The max number of rows returned from a remote operation
    * before its evicted from a cache.
    */
   val maxOperationRowCount:Int = 10,

   /**
    * The max number of remote call invocations that are stored per cache
    * By default, each query gets it's own cache (unless configured with a @Cache configuration on the query)
    */
   val maxCachedOperations:Int = DEFAULT_MAX_CACHED_OPERATIONS,

   /**
    * The max duration that a remote call is held in the cache
    * By default, each query gets it's own cache (unless configured with a @Cache configuration on the query)
    */
   val cachedOperationTtl:Duration = DEFAULT_MAX_DURATION
) {
   companion object {
      val DEFAULT_MAX_CACHED_OPERATIONS = 1000
      val DEFAULT_MAX_DURATION = Duration.ofMinutes(60)
   }
}
