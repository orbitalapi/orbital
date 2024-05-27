package com.orbitalhq.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.nio.serialization.compact.CompactReader
import com.hazelcast.nio.serialization.compact.CompactSerializer
import com.hazelcast.nio.serialization.compact.CompactWriter
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.serde.SerializableTypedInstance
import com.orbitalhq.models.serde.toSerializable
import com.orbitalhq.query.CacheExchange
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.connectors.CacheNames
import com.orbitalhq.query.connectors.OperationCacheKey
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.fqn
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit


/**
 * Caching provider which writes results to a Hazelcast IMap.
 *
 * Previous implementation at ee9362e39d1bf3b0e0ecdc6ab6a4e03e82efe701
 *
 * We have tried multiple designs here, with a desire to keep responses
 * from the cache streaming (and reactive) so the 2nd caller can receive messages
 * immediately from the Flux, without waiting for the entire response to be cached.
 *
 * Unfortunately, we couldn't find a design that didn't rely on events within Hazelcast.
 * (Using Topic / ReliableTopic / EntryListener) all use RingBuffer under the hood, which
 * will drop events if it gets full. This would mean consumers are at risk of not receiving
 * the full response, making it an unviable solution.
 *
 * We've had to fall back to loading the data atomically, and subsequent callers have to wait
 * (non-blocking) for the response to be loaded.
 *
 * Design doc at:
 * https://docs.google.com/document/d/11UGpwYntioho54xZsGUU_7a89cc38n7S-3QNSdFSgqo/edit#heading=h.6sd5ghm66gfy
 */
class HazelcastMapCachingProvider(
   hazelcast: HazelcastInstance,
   private val schemaStore: SchemaStore,
   private val connectionName: String,
   private val connectionAddress: String,
   private val defaultTTL: Duration = Duration.ofMinutes(60),
   private val clock: Clock = Clock.systemUTC()
) : HazelcastCachingProvider(hazelcast) {
   companion object {
      const val OPERATION_CACHE_NAME = "operationCache"
      private val logger = KotlinLogging.logger {}
   }

   private val map = hazelcast.getMap<String, ExpiringByteArray>(OPERATION_CACHE_NAME)

   // see startTtlUpdateListener...
   private val updateTtlSink = Sinks.many().unicast().onBackpressureError<UpdateTtlEvent>()

   init {
      startTtlUpdateListener()
   }

   /**
    * Creates a subscriber that updates TTL's on map entries.
    * When we load (from a loader function), we use compute() (see load for a discussion on why).
    * However, there's no API to compute() and set the TTL on the value.
    * Therefore, when a loader executes, we trigger to set the TTL asyncronously later.
    *
    * Note - we delay setting the TTL for 1 second to ensure the value is present.
    *
    * If setting a TTL fails (because the entry is not present yet), we
    * retry up to 5 times, then abort.
    */
   private fun startTtlUpdateListener() {
      updateTtlSink.asFlux()
         .delayElements(Duration.ofSeconds(1))
         .subscribe { event ->
            val ttlUpdated = map.setTtl(event.key, event.ttlMillisecondsFromNow(clock), TimeUnit.MILLISECONDS)
            if (!ttlUpdated) {
               if (event.canRetryIfFailed) {
                  logger.info { "Failed to update the TTL for key ${event.key} (attempt ${event.attemptCount + 1}) - will retry" }
                  updateTtlSink.emitNext(
                     event.asRetryEvent(),
                     Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(5))
                  )
               } else {
                  logger.info { "Failed to update the TTL for key ${event.key}. Max retries exceeded, the item will be tagged for eviction from the cache" }
                  // Last ditch attempt
                  map.setTtl(event.key, 1, TimeUnit.MILLISECONDS)
               }

            }
         }
   }

   override fun load(
      key: OperationCacheKey,
      message: OperationInvocationParamMessage,
      loader: () -> Flux<TypedInstance>
   ): Flux<TypedInstance> {

      val startTime = System.nanoTime()
      val resultsFromLoader = mutableListOf<TypedInstance>()

      // Use Mono.create() to ensure the map reading below doesn't happen
      // on the main thread
      return Mono.create { sink ->




         // Design choice: use map.compute()...
         // We've tried multiple approaches here.
         // We need an async, atomic way to ensure the loader is only invoked once.
         // We tried using an EntryProcessor, but access to the loader is not serializable, so
         // cannot be run remotely.
         // compute(key) promises atomic processing (so the loader is only invoked once),
         // and runs locally on the client.
         val byteArray = map.computeIfAbsent(key) { invokeLoader(key, loader, resultsFromLoader) }!!
         sink.success(byteArray)
      }.map { expiringByteArray ->

         // Edge case: We loaded a value from the cache which is already expired.
         // In this case, force a reload using compute() vs computeIfAbsent()
         if (expiringByteArray.isExpired(clock)) {
            map.compute(key) { key, _ ->
               invokeLoader(key, loader, resultsFromLoader)
            }!!
         } else expiringByteArray
      }
         .flatMapIterable { expiringByteArray ->

            if (resultsFromLoader.isNotEmpty()) {
               // If the resultsFromLoader are populated, it means this thread
               // was the "writer", populating the cache.
               // Return the results directly from the upstream invoker,
               // so that values like lineage / dataSources remain populated correctly
               resultsFromLoader
            } else {
               // ... otherwise we fetched from the cache.
               // This means that we should update the datasource so it shows
               // the cache call and notify the eventDispatcher
               val byteArrays = expiringByteArray.value

               val (operationResult, dataSource) = createDatasource(startTime, message, key, byteArrays.size)
               message.eventDispatcher.reportRemoteOperationInvoked(operationResult, message.queryId)

               val typedInstances = byteArrays.map { byteArray ->
                  SerializableTypedInstance.fromBytes(byteArray)
                     .toTypedInstance(schemaStore.schema(), dataSource = dataSource)
               }
               typedInstances
            }
         }
         .subscribeOn(Schedulers.boundedElastic())
   }

   private fun invokeLoader(
      key: String,
      loader: () -> Flux<TypedInstance>,
      resultsFromCacheMiss: MutableList<TypedInstance>
   ): ExpiringByteArray {
      val invocationReturnValue = loader.invoke()
      val resultValues: MutableList<Pair<ByteArray, Instant?>> = (invocationReturnValue
         .mapNotNull { typedInstance ->
            resultsFromCacheMiss.add(typedInstance)
            val expiration = typedInstance.metadata[TypedInstance.EXPIRY_METADATA] as? Instant
            val bytes = typedInstance.toSerializable().toBytes()
            bytes to expiration
         }
         .collectList()
         .block(Duration.ofSeconds(60)))!!

      val (expirationTime, ttl) = calculateTtlFromResults(resultValues, defaultTTL, clock)
      val resultValueBytes = resultValues.map { it.first }

      val expiringByteArray = ExpiringByteArray(expirationTime, resultValueBytes)

      // We can't directly set the TTL here, so we queue an instruction to set the ttl in a bit
      updateTtlSink.emitNext(
         UpdateTtlEvent(key, expirationTime),
         Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(10))
      )
      return expiringByteArray
   }


   private fun createDatasource(
      startTime: Long,
      message: OperationInvocationParamMessage,
      cacheKey: OperationCacheKey,
      resultSize: Int,
   ): Pair<OperationResult, OperationResultDataSourceWrapper> {
      val remoteCall = RemoteCall(
         service = CacheNames.cacheServiceName(connectionName).fqn(),
         address = connectionAddress,
         operation = CacheNames.CACHE_READ_OPERATION_NAME,
         method = CacheNames.CACHE_READ_OPERATION_NAME,
         durationMs = Duration.ofNanos(System.nanoTime() - startTime).toMillis(),
         exchange = CacheExchange(
            connectionName,
            message.operation.name,
            cacheKey,
            CacheExchange.CacheOperationVerb.LOOKUP,
            CacheExchange.CacheType.Hazelcast,
            resultSize
         ),
         timestamp = Instant.ofEpochMilli(Duration.ofNanos(startTime).toMillis()),
         response = null, // Do we want to persist the response again?
         responseMessageType = ResponseMessageType.FULL,
         responseTypeName = message.operation.returnType.name
      )


      // Do we always get ConstructedQueryDataSource here? If so below check is redundant. QueryProfileChartBuilder
      val parameters = message.parameters
      val isConstructedQueryDataSource =
         parameters.isNotEmpty() && parameters[0].second.let { it.source is ConstructedQueryDataSource }
      val operationResult = if (isConstructedQueryDataSource) {
         val constructedQueryDataSource = parameters[0].second.let { it.source as ConstructedQueryDataSource }
         OperationResult.fromTypedInstances(
            constructedQueryDataSource.inputs,
            remoteCall
         )
      } else {
         OperationResult.from(parameters, remoteCall)
      }

      val dataSource = OperationResultDataSourceWrapper(operationResult)
      return Pair(operationResult, dataSource)
   }


   override fun evict(operationKey: OperationCacheKey) {
      map.remove(operationKey)
   }
}

/**
 * A entry for our map containing an explicit expiration time.
 * We track this because of a race condition where Hazelcast has not yet removed an entry with a TTL,
 * even though it has expired.
 *
 * In this scenario, we need to treat the value as a cache miss, and re-load from the loader
 */
data class ExpiringByteArray(val expiresAt: Long?, val value: List<ByteArray>) {
   constructor(expiresAt: Instant?, value: List<ByteArray>) : this(expiresAt?.toEpochMilli(), value)

   fun isExpired(clock: Clock): Boolean {
      return when {
         expiresAt == null -> false
         else -> expiresAt < clock.instant().toEpochMilli()
      }
   }
}

private data class UpdateTtlEvent(val key: String, val expiresAt: Instant, val attemptCount: Int = 0) {
   companion object {
      private const val MAX_ATTEMPTS = 5
   }

   fun isExpired(clock: Clock) = expiresAt.isBefore(clock.instant())

   fun ttlMillisecondsFromNow(clock: Clock): Long {
      return if (isExpired(clock)) {
         // If the item is already expired, set the TTL 1ms from now,
         // as setting to 0 or negative means the value is never expired
         1
      } else {
         Duration.between(clock.instant(), expiresAt).toMillis()
      }
   }

   val canRetryIfFailed = attemptCount < MAX_ATTEMPTS
   fun asRetryEvent(): UpdateTtlEvent = this.copy(attemptCount = attemptCount + 1)

}

class ExpiringByteArraySerializer : CompactSerializer<ExpiringByteArray> {
   override fun read(reader: CompactReader): ExpiringByteArray {
      val expiresAt = reader.readNullableInt64("expiresAt")
      val listSize = reader.readInt32("listSize")
      val byteArrayList = mutableListOf<ByteArray>()
      for (i in 0 until listSize) {
         val byteArray = reader.readArrayOfInt8("byteArray_$i")!!
         byteArrayList.add(byteArray)
      }
      return ExpiringByteArray(expiresAt, byteArrayList)
   }

   override fun getTypeName(): String = ExpiringByteArray::class.java.name

   override fun getCompactClass(): Class<ExpiringByteArray> = ExpiringByteArray::class.java

   override fun write(writer: CompactWriter, value: ExpiringByteArray) {
      writer.writeNullableInt64("expiresAt", value.expiresAt)
      writer.writeInt32("listSize", value.value.size)
      value.value.forEachIndexed { index, bytes ->
         writer.writeArrayOfInt8("byteArray_$index", bytes)
      }
   }
}

private fun calculateTtlFromResults(
   resultValues: List<Pair<ByteArray, Instant?>>,
   defaultTTL: Duration,
   clock: Clock
): Pair<Instant, Duration> {
   val expirationTime = resultValues.mapNotNull { it.second }
      .minOrNull() // use the earliest expiration time
   //?.let { expirationTime -> Duration.between(Instant.now(), expirationTime) }

   return when {
      expirationTime != null && expirationTime.isAfter(clock.instant()) -> expirationTime to Duration.between(
         clock.instant(),
         expirationTime
      )

      // The expiration time is before now - ie., it's already expired.
      expirationTime != null -> ALREADY_EXPIRED to Duration.ZERO
      else -> clock.instant().plus(defaultTTL) to defaultTTL
   }
}

private val ALREADY_EXPIRED = Instant.ofEpochMilli(0L)
