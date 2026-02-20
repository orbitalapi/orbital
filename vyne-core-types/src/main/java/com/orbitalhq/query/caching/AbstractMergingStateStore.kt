package com.orbitalhq.query.caching

import com.orbitalhq.models.MixedSources
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObjectFactory
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.types.ParameterizedName
import lang.taxi.types.SumType
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * A state store that uses a ConcurrentHashMap as it's backing store.
 * There's no eviction of either the collection of state stores, or the
 * items within the individual state stores.
 *
 * This is useful for testing, but not suitable for production.
 *
 * TODO : Create a cache backed state store, that has a configurable eviction policy.
 */
class MapBackedStateStoreProvider : StateStoreProvider {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val stateStores = ConcurrentHashMap<String, DefaultMapBasedStateStore>()
   override fun getStateStore(
      stateStoreConfig: StateStoreConfig,
      sumType: SumType,
      schema: Schema,
      emitMode: StateStoreProvider.EmitMode,
      namePrefix: String,
   ): StateStore? {
      if (!stateStoreConfig.connection.isNullOrBlank()) {
         logger.warn { "Received request for state store against connection ${stateStoreConfig.connection} - defaultStateStoreProvider does not support connection-bound state stores" }
      }
      return stateStores.getOrPut(getStateStoreKey(namePrefix, sumType, emitMode)) {
         DefaultMapBasedStateStore(schema, sumType, emitMode)
      }
   }

}

class DefaultMapBasedStateStore(
   private val schema: Schema,
   private val sumType: SumType,
   emitMode: StateStoreProvider.EmitMode,
) : AbstractMergingStateStore(
   schema.type(sumType),
   StateStoreIdProvider(sumType, schema),
   HashMapStateBackingStore(),
   emitMode
) {
   override fun mergeNotNullValues(typedInstance: TypedInstance): Mono<TypedInstance> {
      return mergeNotNullValues(typedInstance, schema)
   }
}

abstract class AbstractMergingStateStore(
   private val sumType: Type,
   private val idProvider: StateStoreIdProvider,
   private val backingStore: StateBackingStore,
   private val emitMode: StateStoreProvider.EmitMode
) :
   StateStore {

   companion object {
      fun buildSumTypeFromValues(sumType: Type, values: List<TypedInstance>, schema: Schema): TypedInstance {
         return kotlinx.coroutines.runBlocking {
            TypedObjectFactory(
               sumType,
               FactBag.of(values, schema),
               schema,
               source = MixedSources,
               // This is important.
               // We take the last value when reading from a fact bag.
               // More recent values overwrite older values
               // This works down the tree - two different types with children fields of
               // the same type will pick the more recently inserted
               factBagSearchStrategy = FactDiscoveryStrategy.ANY_DEPTH_TAKE_LAST
            )
               .build()
         }
      }
   }

   private val sumTypeMembers = (sumType.taxiType as SumType).types

   protected fun mergeNotNullValues(
      typedInstance: TypedInstance,
      schema: Schema,

      ): Mono<TypedInstance> {
      val itemKey = calculateItemKey(typedInstance) ?: return Mono.just(typedInstance)

      return backingStore.merge(itemKey, typedInstance, schema)
         .flatMap { latestTypedInstances ->
            if (latestTypedInstances.size != sumTypeMembers.size && emitMode == StateStoreProvider.EmitMode.AFTER_ALL_EVENTS) {
               Mono.empty()
            } else {
               val updatedTypedInstance = buildSumTypeFromValues(sumType, latestTypedInstances, schema)
               Mono.just(updatedTypedInstance)
            }

         }
   }

   private fun calculateItemKey(typedInstance: TypedInstance): String? {
      return idProvider.calculateStateStoreId(typedInstance)
   }
}

/**
 * A backing store used in a state store.
 * Responsible for holding the actual state.
 *
 * Invoked each time a new value arrives from one of the streams in a merged stream.
 * Needs to hold the latest state from each of the different stream values, and
 * return a list containing all the current state of items.
 *
 * It's up to the backing store to manage things like clean up, overflow, serialization, etc.
 */
interface StateBackingStore {
   fun merge(itemKey: String, streamValue: TypedInstance, schema: Schema): Mono<List<TypedInstance>>
}

class HashMapStateBackingStore(
   private val map: ConcurrentMap<String, MutableMap<ParameterizedName, TypedInstance>> = ConcurrentHashMap()
) : StateBackingStore {
   override fun merge(itemKey: String, streamValue: TypedInstance, schema: Schema): Mono<List<TypedInstance>> {
      val valuesByTypeName = map.compute(itemKey) { _, storedValue ->
         if (storedValue == null) {
            // Note: Setting access order means that updates to a given key changes the iteration order.
            // This is what we want.
            val map = LinkedHashMap<ParameterizedName,TypedInstance>(5, 0.75f, true)
            map[streamValue.type.paramaterizedName] = streamValue
            map
         } else {
            storedValue[streamValue.type.paramaterizedName] = streamValue
            storedValue
         }
      }
      if (valuesByTypeName == null) {
         error("Expected a map of latest values by type, but got null")
      }
      return Mono.just(valuesByTypeName.values.toList())
   }

}
