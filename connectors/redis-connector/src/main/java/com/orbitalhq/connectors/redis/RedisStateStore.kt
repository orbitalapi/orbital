package com.orbitalhq.connectors.redis

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.serde.SerializableTypedInstance
import com.orbitalhq.models.serde.toSerializable
import com.orbitalhq.query.caching.AbstractMergingStateStore
import com.orbitalhq.query.caching.StateBackingStore
import com.orbitalhq.query.caching.StateStore
import com.orbitalhq.query.caching.StateStoreConfig
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.schemas.Schema
import io.lettuce.core.api.StatefulRedisConnection
import lang.taxi.types.ParameterizedName
import lang.taxi.types.SumType
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides Redis-backed state stores for storing interim state during stream merging
 */
class RedisStateStoreProvider(
   private val redisConnectionsManager: RedisConnectionsManager
) : StateStoreProvider {

   private val redisStores = ConcurrentHashMap<String, RedisStateStore>()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun getStateStore(
      stateStoreConfig: StateStoreConfig,
      sumType: SumType,
      schema: Schema,
      emitMode: StateStoreProvider.EmitMode,
      namePrefix: String
   ): StateStore? {
      val (connection, config) = redisConnectionsManager.redisConnection(stateStoreConfig.connection)
      val storeKey = stateStoreConfig.name
         ?: getStateStoreKey(
            prefix = "StateStore_${stateStoreConfig.connection.orEmpty()}",
            sumType,
            emitMode
         )

      return redisStores.getOrPut(storeKey) {
         logger.info { "Creating new Redis state store with key $storeKey" }
         RedisStateStore(
            connection,
            storeKey,
            stateStoreConfig.maxIdleSeconds,
            schema
         )
      }
   }

   private fun getStateStoreKey(
      prefix: String,
      sumType: SumType,
      emitMode: StateStoreProvider.EmitMode
   ): String {
      val typeNames = sumType.types.joinToString("_") { it.typeName.parameterizedName }
      return "${prefix}_${typeNames}_${emitMode.name}"
   }
}

/**
 * Redis-backed state store implementation
 */
class RedisStateStore(
   private val connection: StatefulRedisConnection<String, String>,
   private val keyPrefix: String,
   private val maxIdleSeconds: Int,
   private val schema: Schema
) : AbstractMergingStateStore(), StateBackingStore<SerializableTypedInstance> {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun get(key: Any): Mono<SerializableTypedInstance?> {
      return Mono.fromCallable {
         val redisKey = buildRedisKey(key)
         val value = connection.sync().get(redisKey)
         if (value != null) {
            deserialize(value)
         } else {
            null
         }
      }
   }

   override fun put(key: Any, value: SerializableTypedInstance): Mono<Void> {
      return Mono.fromRunnable {
         val redisKey = buildRedisKey(key)
         val serialized = serialize(value)
         connection.sync().setex(redisKey, maxIdleSeconds.toLong(), serialized)
      }
   }

   override fun merge(key: Any, remappingFunction: (SerializableTypedInstance?) -> SerializableTypedInstance?): Mono<SerializableTypedInstance?> {
      return Mono.fromCallable {
         val redisKey = buildRedisKey(key)

         // Get current value
         val currentJson = connection.sync().get(redisKey)
         val current = currentJson?.let { deserialize(it) }

         // Apply remapping function
         val newValue = remappingFunction(current)

         if (newValue != null) {
            // Store new value
            val serialized = serialize(newValue)
            connection.sync().setex(redisKey, maxIdleSeconds.toLong(), serialized)
         } else if (current != null) {
            // Delete if remapping returned null
            connection.sync().del(redisKey)
         }

         newValue
      }
   }

   override fun stateBackingStore(): StateBackingStore<SerializableTypedInstance> = this

   override fun toTypedInstance(storeValue: SerializableTypedInstance): TypedInstance {
      return storeValue.toTypedInstance(schema)
   }

   override fun toStoreValue(typedInstance: TypedInstance): SerializableTypedInstance {
      return typedInstance.toSerializable()
   }

   private fun buildRedisKey(key: Any): String {
      return "$keyPrefix:$key"
   }

   private fun serialize(value: SerializableTypedInstance): String {
      // Using JSON serialization for simplicity
      return value.toJson()
   }

   private fun deserialize(json: String): SerializableTypedInstance {
      return SerializableTypedInstance.fromJson(json)
   }
}
