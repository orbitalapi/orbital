package com.orbitalhq.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.serde.SerializableTypedInstance
import com.orbitalhq.models.serde.toSerializable
import com.orbitalhq.query.caching.StateStore
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.query.caching.calculateStateStoreId
import com.orbitalhq.query.caching.mergeNonNullValues
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.Schema
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides Hazelcast-backed cache stores,
 * which are used for things like storing interim state when merging streams
 */
class HazelcastStateStoreProvider(
   private val connectors: SourceLoaderConnectorsRegistry,
) : StateStoreProvider {

   private val hazelcastStores = ConcurrentHashMap<String, HazelcastStateStore>()

   override fun getCacheStore(connectionName: String, key: String, schema: Schema): StateStore? {
      val connectorsConfig = connectors.load()
      val hazelcastConfig = if (connectorsConfig.hazelcast.keys.isNotEmpty()) {
         connectorsConfig.hazelcast[connectorsConfig.hazelcast.keys.first()]!!
      } else return null

      val cacheStoreKey = connectionName + key
      val cacheStore = hazelcastStores.getOrPut(cacheStoreKey) {
         val hazelcastInstnace = HazelcastBuilder.build(hazelcastConfig)
         HazelcastStateStore(hazelcastInstnace, key, schema)
      }
      return cacheStore
   }
}


class HazelcastStateStore(
   private val hazelcastInstance: HazelcastInstance,
   private val mapName: String,
   private val schema: Schema
) : StateStore {
   override fun mergeNotNullValues(typedInstance: TypedInstance): Mono<TypedInstance> {
      val map = hazelcastInstance.getMap<String, ByteArray>(mapName)
      val itemKey = calculateItemKey(typedInstance, schema) ?: return Mono.just(typedInstance)

      return Mono.create<TypedInstance> { sink ->

         // We store byte[], which means we need to
         // convert the merged value into a TypedInstance.
         // Since we already have the merged value inside the compute{} function,
         // we don't want to do another round trip of deserialization.
         // However, we want to preserve the correct operation order, avoiding calling
         // mono.complete() too early.
         // So, we store the state here.
         var updatedTypedInstance: TypedInstance? = null

         val updated = map.compute(itemKey) { _, storedValue ->
            if (storedValue == null) {
               updatedTypedInstance = typedInstance
               return@compute typedInstance.toSerializable().toBytes()
            }
            val storedTypedInstance = SerializableTypedInstance.fromBytes(storedValue).toTypedInstance(schema)
            val mergedValue = merge(typedInstance, storedTypedInstance)
            // TODO : This is probably bad - calling sink.success before the
            // map function has returned.
            updatedTypedInstance = mergedValue
            mergedValue.toSerializable().toBytes()
         }
         if (updated == null) {
            error("Received null after merge operation - how does this happen?")
         }
         if (updatedTypedInstance == null) {
            error("Merged TypedInstance was null after merge operation - how does this happen?")
         }

         sink.success(updatedTypedInstance)
      }

   }

   private fun merge(newValue: TypedInstance, previousValue: TypedInstance): TypedInstance {
      return mergeNonNullValues(previousValue, newValue, schema)
   }

   private fun calculateItemKey(typedInstance: TypedInstance, schema: Schema): String? {
      return calculateStateStoreId(typedInstance, schema)
   }

}
