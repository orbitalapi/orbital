package com.orbitalhq.connectors.hazelcast

import arrow.core.Either
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.connectors.CacheAwareOperationInvocationDecorator
import com.orbitalhq.query.connectors.OperationCacheKey
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.schema.consumer.SchemaStore
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration

abstract class HazelcastCachingProvider(protected val hazelcast: HazelcastInstance) {
   abstract fun load(
      key: OperationCacheKey,
      message: OperationInvocationParamMessage,
      loader: () -> Flux<Either<StreamErrorMessage, TypedInstance>>
   ): Flux<TypedInstance>

   abstract fun evict(operationKey: OperationCacheKey)
}

object HazelcastCacheProviderFactory {
   fun instance(
      hazelcast: HazelcastInstance,
      schemaStore: SchemaStore,
      connectionName: String,
      connectionAddress: String,
      clock: Clock,
      ttl: Duration = CacheAwareOperationInvocationDecorator.DEFAULT_CACHE_TTL
   ): HazelcastCachingProvider {
      return HazelcastMapCachingProvider(
         hazelcast,
         schemaStore,
         connectionName,
         connectionAddress,
         clock = clock,
         defaultTTL = ttl
      )
   }
}
