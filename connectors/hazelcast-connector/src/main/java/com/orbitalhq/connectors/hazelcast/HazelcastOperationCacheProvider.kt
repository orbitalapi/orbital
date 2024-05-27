package com.orbitalhq.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.connectors.CachingOperatorInvoker
import com.orbitalhq.query.connectors.OperationCacheKey
import com.orbitalhq.query.connectors.OperationCacheProvider
import com.orbitalhq.query.connectors.OperationCacheProviderBuilder
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.CachingStrategy
import com.orbitalhq.schemas.RemoteCache
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration

private val logger = KotlinLogging.logger {}
class HazelcastOperationCacheProvider(
   private val hazelcast: HazelcastInstance,
   private val schemaStore: SchemaStore,
   private val maxSize: Int = 10,
   private val connectionName: String,
   private val connectionAddress: String,
   private val clock: Clock = Clock.systemUTC(),
) :
   OperationCacheProvider {
   companion object {
      val COMPLETION_MARKER_KEY = "#"
      val COMPLETION_MARKER = COMPLETION_MARKER_KEY.toByteArray()
   }

   init {
      Flux.from(schemaStore.schemaChanged)
         .subscribe {
            // TODO : Clear the cacahes
            logger.warn { "Hazelcast cache cleaning not implemented - old responses still cached" }
//            logger.info { "Schema changed, so invalidating cache ${list.name}" }
//            list.clear()
         }
   }

   override fun getCachingInvoker(
      operationKey: OperationCacheKey,
      invoker: OperationInvoker
   ): CachingOperatorInvoker {

      return CachingOperatorInvoker(
         operationKey, invoker, this::load
      )
   }

   private fun load(
      key: OperationCacheKey,
      message: OperationInvocationParamMessage,
      loader: () -> Flux<TypedInstance>
   ): Flux<TypedInstance> {
      return HazelcastCacheProviderFactory
         .instance(hazelcast, schemaStore, connectionName, connectionAddress, clock)
         .load(key, message, loader)
   }

   override fun evict(operationKey: OperationCacheKey) {
      HazelcastCacheProviderFactory
         .instance(hazelcast, schemaStore, connectionName, connectionAddress, clock)
         .evict(operationKey)
   }
}

/**
 * Builds Hazelcast backed cache providers that are used for
 * caching results of operation calls.
 */
class HazelcastOperationCacheBuilder(
   private val hazelcastConnectionsManager: HazelcastConnectionsManager,
   private val schemaStore: SchemaStore,
) :
   OperationCacheProviderBuilder {
   override fun canBuild(strategy: CachingStrategy): Boolean {
      if (strategy !is RemoteCache) return false
      return hazelcastConnectionsManager.canProvideHazelcastInstance(strategy.connectionName)
   }

   override fun buildOperationCache(strategy: CachingStrategy,  maxCachedOperations: Int, cachedOperationTtl: Duration): OperationCacheProvider {
      require(strategy is RemoteCache)
      val (client, config) = hazelcastConnectionsManager.hazelcastConnection(strategy.connectionName)
      return HazelcastOperationCacheProvider(
         client,
         schemaStore,
         maxCachedOperations,
         config.connectionName,
         config.addresses.joinToString(",")
      )
   }
}
