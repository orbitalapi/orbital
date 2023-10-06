package com.orbitalhq.connectors.hazelcast

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.collection.IList
import com.hazelcast.collection.ItemEvent
import com.hazelcast.collection.ItemListener
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.cp.lock.FencedLock
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.serde.SerializableTypedInstance
import com.orbitalhq.models.serde.toSerializable
import com.orbitalhq.query.connectors.*
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.CachingStrategy
import com.orbitalhq.schemas.RemoteCache
import com.orbitalhq.utils.Ids
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.random.Random

class HazelcastCacheProvider(
   private val hazelcast: HazelcastInstance,
   private val schemaStore: SchemaStore,
   private val maxSize: Int = 10
) :
   OperationCacheProvider {
   companion object {
      private val LOADING_MARKER = "$".toByteArray()
      private val COMPLETION_MARKER = "#".toByteArray()
      private val logger = KotlinLogging.logger {}
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

   override fun getCachingInvoker(operationKey: OperationCacheKey, invoker: OperationInvoker): CachingOperatorInvoker {

      return CachingOperatorInvoker(
         operationKey, invoker, maxSize, this::load
      )
   }

   private fun load(key: OperationCacheKey, loader: () -> Flux<TypedInstance>): Flux<TypedInstance> {
      val list = hazelcast.getList<ByteArray>(key)

      val lockMap = hazelcast.getMap<String,String>("operationLocks")
      val thisSeed = Ids.fastUuid()

      return if (list.isEmpty() && lockMap.getOrPut(key) { thisSeed } == thisSeed) {
         // We're the writer
         createFluxFromLoader(loader, list)
            .doFinally {
               lockMap.remove(key)
            }
      } else {
         createFluxFromCache(list)
      }
   }

   private fun createFluxFromCache(list:IList<ByteArray>): Flux<TypedInstance> = Flux.create { sink ->
      // This method returns the cached value.
      // It's not currently "correct", as a race condition exists
      // where inflight values might be missed between us checking to see if the cached value
      // is "complete" (has our special completionMarker), and subscribing for changes.
      // Not sure the best way to handle that yet.

      // Initial state
      var foundCompletionMarker = false
      list.filter {
         val isCompletionMarker = it.contentEquals(COMPLETION_MARKER)
         foundCompletionMarker = foundCompletionMarker || isCompletionMarker
         !isCompletionMarker
      }.map {
         SerializableTypedInstance.fromBytes(it).toTypedInstance(schemaStore.schema())
      }.forEach { sink.next(it) }

      if (foundCompletionMarker) {
         sink.complete()
      } else {
         // Listen for changes.
         var listenerId: UUID? = null
         val listener: ItemListener<ByteArray> = object : ItemListener<ByteArray> {
            override fun itemAdded(event: ItemEvent<ByteArray>) {
               if (event.item.contentEquals(COMPLETION_MARKER)) {
                  sink.complete()
                  listenerId?.let { uuid -> list.removeItemListener(uuid) }
               } else {
                  SerializableTypedInstance.fromBytes(event.item).toTypedInstance(schemaStore.schema())
               }
            }

            override fun itemRemoved(item: ItemEvent<ByteArray>?) {
            }
         }
         listenerId = list.addItemListener(listener, true)
      }


   }

   private fun createFluxFromLoader(loader: () -> Flux<TypedInstance>, list: IList<ByteArray>): Flux<TypedInstance> {
      val flux = loader()
      return flux.doOnNext { next ->
         try {
            list.add(next.toSerializable().toBytes())
         } catch (e: Exception) {
            logger.error(e) { "Failed to write TypedInstance to cache" }
         }
      }.doFinally {

         list.add(COMPLETION_MARKER)
      }
   }


   override fun evict(operationKey: OperationCacheKey) {
      val list = hazelcast.getList<ByteArray>(operationKey)
      if (list != null) {
         logger.info { "Hazelcast cache $operationKey has been cleared" }
         list.clear()
      } else {
         logger.warn { "Cannot evict list $operationKey as it was not found" }
      }
   }
}

class HazelcastCacheProviderBuilder(
   private val connectors: SourceLoaderConnectorsRegistry,
   private val schemaStore: SchemaStore,
   private val maxSize: Int = 10,
) :
   OperationCacheProviderBuilder {
   private val hazelcastConnections = ConcurrentHashMap<String, HazelcastInstance>()

   override fun canBuild(strategy: CachingStrategy): Boolean {
      if (strategy !is RemoteCache) return false
      return connectors.load().hazelcast.containsKey(strategy.connectionName)
   }

   override fun build(strategy: CachingStrategy, maxSize: Int): OperationCacheProvider {
      require(strategy is RemoteCache)
      val client = hazelcastConnections.getOrPut(strategy.connectionName) {
         val connectors = connectors.load()
         require(connectors.hazelcast.containsKey(strategy.connectionName)) { "No connection for Hazelcast named ${strategy.connectionName} exists" }

         val connectionConfig = connectors.hazelcast[strategy.connectionName]!!
         val clientConfig = ClientConfig().apply {
            networkConfig.addAddress(*connectionConfig.addresses.toTypedArray())
         }
         HazelcastClient.newHazelcastClient(clientConfig)
      }

      return HazelcastCacheProvider(client, schemaStore, maxSize)
   }

}
