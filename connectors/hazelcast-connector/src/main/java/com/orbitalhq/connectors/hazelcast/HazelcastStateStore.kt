package com.orbitalhq.connectors.hazelcast

import com.hazelcast.config.MapConfig
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.serde.SerializableTypedInstance
import com.orbitalhq.models.serde.toSerializable
import com.orbitalhq.query.caching.AbstractMergingStateStore
import com.orbitalhq.query.caching.StateBackingStore
import com.orbitalhq.query.caching.StateStore
import com.orbitalhq.query.caching.StateStoreConfig
import com.orbitalhq.query.caching.StateStoreIdProvider
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.schemas.Schema
import lang.taxi.types.ParameterizedName
import lang.taxi.types.SumType
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides Hazelcast-backed cache stores,
 * which are used for things like storing interim state when merging streams
 */
class HazelcastStateStoreProvider(
   private val hazelcastConnectionsManager: HazelcastConnectionsManager,
) : StateStoreProvider {
   private val hazelcastStores = ConcurrentHashMap<String, HazelcastStateStore>()
   companion object {
      fun setMapConfig(hazelcastInstance: HazelcastInstance, stateStoreConfig: StateStoreConfig, cacheStoreKey: String) {
         val mapConfig = MapConfig(
            cacheStoreKey,
         )
         mapConfig.setMaxIdleSeconds(stateStoreConfig.maxIdleSeconds)
         hazelcastInstance.config.addMapConfig(mapConfig)
      }
   }
   override fun getStateStore(
      stateStoreConfig: StateStoreConfig,
      sumType: SumType,
      schema: Schema,
      emitMode: StateStoreProvider.EmitMode,
      namePrefix: String
   ): StateStore? {
      val (hazelcastInstance, connectionsConfig) = hazelcastConnectionsManager.hazelcastConnection(stateStoreConfig.connection)
      val cacheStoreKey = stateStoreConfig.name ?: getStateStoreKey(prefix = "StateStore_${stateStoreConfig.connection.orEmpty()}", sumType, emitMode)
      val cacheStore = hazelcastStores.getOrPut(cacheStoreKey) {
         setMapConfig(hazelcastInstance, stateStoreConfig, cacheStoreKey)
         val map = hazelcastInstance.getMap<String, Map<ParameterizedName, ByteArray>>(cacheStoreKey)
         HazelcastStateStore(map, schema, sumType, emitMode)
      }
      val mapConfig = hazelcastInstance.config.getMapConfig(cacheStoreKey)
      if (mapConfig != null && mapConfig.maxIdleSeconds != stateStoreConfig.maxIdleSeconds) {
         // We can't reconfigure a map once it's been created.
         // Send a meaningful error
         val name = stateStoreConfig.name?.let { "named $it" } ?: "with the default name"
         error("A state store $name already exists, but with a different maxIdleSeconds (${mapConfig.maxIdleSeconds}). The connected StateStore (Hazelcast) does not permit changing maxIdleSeconds once configured. Please either set a new name for the state store by adding 'name=\"MyNewName\" to your @StateStore annotation, or change the maxIdleSeconds back to ${mapConfig.maxIdleSeconds}")
      }
      return cacheStore
   }

}


class HazelcastStateStore(
   private val map: IMap<String, Map<ParameterizedName, ByteArray>>,
   private val schema: Schema,
   sumType: SumType,
   emitMode: StateStoreProvider.EmitMode
) : AbstractMergingStateStore(
   schema.type(sumType), StateStoreIdProvider(sumType, schema), HazelcastMapStateBackingStore(map), emitMode
) {
   override fun mergeNotNullValues(typedInstance: TypedInstance): Mono<TypedInstance> {
      return mergeNotNullValues(typedInstance, schema)
   }
}

class HazelcastMapStateBackingStore(private val map: IMap<String, Map<ParameterizedName, ByteArray>>) :
   StateBackingStore {
   override fun merge(itemKey: String, streamValue: TypedInstance, schema: Schema): Mono<List<TypedInstance>> {
      return Mono.create { sink ->

         // We store byte[], which means we need to
         // convert the merged value into a TypedInstance.
         // Since we already have the merged value inside the compute{} function,
         // we don't want to do another round trip of deserialization.
         // However, we want to preserve the correct operation order, avoiding calling
         // mono.complete() too early.
         // So, we store the state here.
         var deserializedMapWithUpdate: Map<ParameterizedName, TypedInstance>? = null
         map.compute(itemKey) { _, storedValue ->
            // This map function needs to provide two values:
            // To the outer function we need a Map<PName, TypedInstance> with the actual
            // typed instances
            // The HAzelcast map needs to be be updated with a Map<PName, ByteArray> for
            // the new value.
            // Given serialization is expensive, we try to avoid unneccessary serde
            // of the TypedInstance we've just received in the out function

            val serializedValue = streamValue.toSerializable().toBytes()
            val parameterizedName = streamValue.type.paramaterizedName
            if (storedValue == null) {
               // Avoid extra deserialization hop, just return the value now.
               deserializedMapWithUpdate = mapOf(parameterizedName to streamValue)
               return@compute mapOf(parameterizedName to serializedValue)
            }
            val deserializedMap = storedValue.mapValues { (_, serializedTypeInstance) ->
               SerializableTypedInstance.fromBytes(serializedTypeInstance).toTypedInstance(schema)
            }.toMutableMap()
            deserializedMap[parameterizedName] = streamValue
            deserializedMapWithUpdate = deserializedMap

            val mutableStoredValue = storedValue.toMutableMap()
            mutableStoredValue[parameterizedName] = serializedValue

            mutableStoredValue
         }
         if (deserializedMapWithUpdate == null) {
            // How could this happen?
            error("Invalid state: Value returned from Hazelcast update was null")
         }

         sink.success(deserializedMapWithUpdate!!.values.toList())
      }
   }

}
