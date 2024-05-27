package com.orbitalhq.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.connectors.OperationCacheKey
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.schema.consumer.SchemaStore
import reactor.core.publisher.Flux
import java.time.Clock

abstract class HazelcastCachingProvider(protected val hazelcast: HazelcastInstance) {
   abstract fun load(
      key: OperationCacheKey,
      message: OperationInvocationParamMessage,
      loader: () -> Flux<TypedInstance>
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
   ): HazelcastCachingProvider {
      return HazelcastMapCachingProvider(
         hazelcast,
         schemaStore,
         connectionName,
         connectionAddress,
         clock = clock
      )
   }
}
