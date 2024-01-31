package com.orbitalhq.connectors.hazelcast

import com.hazelcast.collection.IList
import com.hazelcast.collection.ItemEvent
import com.hazelcast.collection.ItemListener
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedInstance.Companion.EXPIRY_METADATA
import com.orbitalhq.models.serde.SerializableTypedInstance
import com.orbitalhq.models.serde.toSerializable
import com.orbitalhq.query.CacheExchange
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.connectors.CacheNames
import com.orbitalhq.query.connectors.CachingOperatorInvoker
import com.orbitalhq.query.connectors.OperationCacheKey
import com.orbitalhq.query.connectors.OperationCacheProvider
import com.orbitalhq.query.connectors.OperationCacheProviderBuilder
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.CachingStrategy
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteCache
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.fqn
import com.orbitalhq.utils.Ids
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.util.UUID

class HazelcastOperationCacheProvider(
   private val hazelcast: HazelcastInstance,
   private val schemaStore: SchemaStore,
   private val maxSize: Int = 10,
   private val connectionName: String,
   private val connectionAddress: String
) :
   OperationCacheProvider {
   companion object {
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

   override fun getCachingInvoker(
      operationKey: OperationCacheKey,
      invoker: OperationInvoker
   ): CachingOperatorInvoker {

      return CachingOperatorInvoker(
         operationKey, invoker, maxSize, this::load
      )
   }

   private fun load(
      key: OperationCacheKey,
      message: OperationInvocationParamMessage,
      loader: () -> Flux<TypedInstance>
   ): Flux<TypedInstance> {
      val startTime = Instant.now()
      val list = hazelcast.getList<ByteArray>(key)

      val lockMap = hazelcast.getMap<String, String>("operationLocks")
      // The "seed" approach here operates as a distributed lock.
      // If the list doesn't exist (or is empty), we do a get/put into the lockMap.
      // If the result returned from the get/put is our seed, it means we're the first ones
      // to attempt to write to this list, so we become the writer.
      val thisSeed = Ids.fastUuid()

      return if (list.isEmpty() && lockMap.getOrPut(key) { thisSeed } == thisSeed) {
         // We're the writer
         createFluxFromLoader(loader, list)
            .doFinally {
               lockMap.remove(key)
            }
      } else {
         createFluxFromCache(list, message, startTime, key)
      }
   }

   private fun createFluxFromCache(
      list: IList<ByteArray>,
      message: OperationInvocationParamMessage,
      startTime: Instant,
      listName: OperationCacheKey
   ): Flux<TypedInstance> = Flux.create { sink ->
      // This method returns the cached value.
      // It's not currently "correct", as a race condition exists
      // where inflight values might be missed between us checking to see if the cached value
      // is "complete" (has our special completionMarker), and subscribing for changes.
      // Not sure the best way to handle that yet.


      val (service: Service,
         operation: RemoteOperation,
         parameters: List<Pair<Parameter, TypedInstance>>,
         eventDispatcher: QueryContextEventDispatcher,
         queryId: String) = message

      // Initial state
      var foundCompletionMarker = false
      val remoteCall = RemoteCall(
         service = CacheNames.cacheServiceName(connectionName).fqn(),
         address = connectionAddress,
         operation = CacheNames.CACHE_READ_OPERATION_NAME,
         method = CacheNames.CACHE_READ_OPERATION_NAME,
         durationMs = Duration.between(startTime, Instant.now()).toMillis(),
         exchange = CacheExchange(
            connectionName,
            message.operation.name,
            listName
         ),
         timestamp = startTime,
         response = null, // Do we want to persist the response again?
         responseMessageType = ResponseMessageType.FULL,
         responseTypeName = operation.returnType.name
      )

      // Do we always get ConstructedQueryDataSource here? If so below check is redundant. QueryProfileChartBuilder
      val isConstructedQueryDataSource = parameters[0].second.let { it.source is ConstructedQueryDataSource }
      val operationResult =  if (isConstructedQueryDataSource) {
         val constructedQueryDataSource = parameters[0].second.let { it.source as ConstructedQueryDataSource }
         OperationResult.fromTypedInstances(
            constructedQueryDataSource.inputs,
            remoteCall
         )
      } else {
         OperationResult.from(parameters, remoteCall)
      }

      val dataSource = OperationResultDataSourceWrapper(operationResult)

      list.filter {
         val isCompletionMarker = it.contentEquals(COMPLETION_MARKER)
         foundCompletionMarker = foundCompletionMarker || isCompletionMarker
         !isCompletionMarker
      }.mapNotNull {
         val typedInstance =
            SerializableTypedInstance.fromBytes(it).toTypedInstance(schemaStore.schema(), dataSource = dataSource)
         val expiredAt = typedInstance.metadata[EXPIRY_METADATA] as? Instant
         if (expiredAt != null && Instant.now().isAfter(expiredAt)) {
            null
         } else {
            // Update the datasource. The existing data source (a DataSourceReference)
            // points to a data source from when this was cached, which is not neccessarily
            // the same query as the current one.
            DataSourceUpdater.update(typedInstance, dataSource)
         }
      }.forEach {
         sink.next(it)
      }

      eventDispatcher.reportRemoteOperationInvoked(
         operationResult,
         queryId = queryId,
      )

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
                  sink.next(SerializableTypedInstance.fromBytes(event.item).toTypedInstance(schemaStore.schema()))
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

/**
 * Builds Hazelcast backed cache providers that are used for
 * caching results of operation calls.
 */
class HazelcastOperationCacheBuilder(
   private val hazelcastConnectionsManager: HazelcastConnectionsManager,
   private val schemaStore: SchemaStore,
   private val maxSize: Int = 10,
) :
   OperationCacheProviderBuilder {
   override fun canBuild(strategy: CachingStrategy): Boolean {
      if (strategy !is RemoteCache) return false
      return hazelcastConnectionsManager.canProvideHazelcastInstance(strategy.connectionName)
   }

   override fun buildOperationCache(strategy: CachingStrategy, maxSize: Int): OperationCacheProvider {
      require(strategy is RemoteCache)
      val (client, config) = hazelcastConnectionsManager.hazelcastConnection(strategy.connectionName)
      return HazelcastOperationCacheProvider(
         client,
         schemaStore,
         maxSize,
         config.connectionName,
         config.addresses.joinToString(",")
      )
   }
}
