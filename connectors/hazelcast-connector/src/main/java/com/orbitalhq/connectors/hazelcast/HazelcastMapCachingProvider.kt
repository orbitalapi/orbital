package com.orbitalhq.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.nio.ObjectDataInput
import com.hazelcast.nio.ObjectDataOutput
import com.hazelcast.nio.serialization.StreamSerializer
import com.hazelcast.nio.serialization.compact.CompactReader
import com.hazelcast.nio.serialization.compact.CompactSerializer
import com.hazelcast.nio.serialization.compact.CompactWriter
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.UndefinedSource
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
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.fqn
import com.orbitalhq.utils.Ids
import com.orbitalhq.utils.StrategyPerformanceProfiler
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
   /**
    * The default TTL for this cache - could be informed by annotations on either the operation
    * or the operation return type, or fallback to DEFAULT_TTL
    */
   private val defaultTTL: Duration,
   private val clock: Clock = Clock.systemUTC()
) : HazelcastCachingProvider(hazelcast) {
   companion object {
      const val OPERATION_CACHE_NAME = "operationCache"
      private val logger = KotlinLogging.logger {}
   }

   private val map = hazelcast.getMap<String, CachedTypedInstanceList>(OPERATION_CACHE_NAME)

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

         // The downside is that there will be more cache-misses (this isn't atomic),
         // but that's an acceptable trade-off,
         // as the near-cache values do not incur (de)/serialization costs
         val typedInstance = map.getOrPut(key) {
            val cachedTypedInstanceList = invokeLoader(key, loader, resultsFromLoader)
            cachedTypedInstanceList
         }!!
         sink.success(typedInstance)
      }.map { typedInstance ->

         // Edge case: We loaded a value from the cache which is already expired.
         // In this case, force a reload using compute() vs computeIfAbsent()
         if (typedInstance.isExpired(clock)) {
            map.compute(key) { key, _ ->
               invokeLoader(key, loader, resultsFromLoader)
            }!!
         } else typedInstance
      }
         .flatMapIterable { cachedTypedInstances ->

            resultsFromLoader.ifEmpty {
               // ... otherwise we fetched from the cache.
               // This means that we should update the datasource so it shows
               // the cache call and notify the eventDispatcher
               val typedInstances = cachedTypedInstances.instances.map { typedInstanceByteArray ->
                  SerializableTypedInstance.fromBytes(typedInstanceByteArray)
                     .toTypedInstance(
                        schemaStore.schema(),
                        dataSource = cachedTypedInstances.dataSource
                     )

               }

               require(cachedTypedInstances.dataSource is CachedOperationResultReference) { "Expected a CachedOperationResultReference, but found ${cachedTypedInstances.dataSource::class.simpleName}" }
               val operationResult =
                  createOperationResult(startTime, message, key, typedInstances.size, cachedTypedInstances.dataSource)
               message.eventDispatcher.reportRemoteOperationInvoked(operationResult, message.queryId)

               typedInstances
            }
         }
         .subscribeOn(Schedulers.boundedElastic())
   }

   private fun invokeLoader(
      key: String,
      loader: () -> Flux<TypedInstance>,
      /**
       * When loading from the actual loader, we populate the real values into
       * this list.
       *
       * This means original data sources are retained
       */
      resultsFromCacheMiss: MutableList<TypedInstance>
   ): CachedTypedInstanceList {
      val invocationReturnValue = loader.invoke()
      val values = invocationReturnValue
         .doOnNext { typedInstance -> resultsFromCacheMiss.add(typedInstance) }
         .map { typedInstance ->
            val expiration = typedInstance.metadata[TypedInstance.EXPIRY_METADATA] as? Instant
            typedInstance to expiration
         }
         .collectList()
         .block(Duration.ofSeconds(60))!!

      val (expirationTime, _) = calculateTtlFromResults(values, defaultTTL, clock)
      val typedInstances = values.map { it.first }
      val operationResultReference: OperationResultReference? = collateDataSources(typedInstances)
      val cachedTypedInstanceList =
         CachedTypedInstanceList(expirationTime, values.map { it.first.toSerializable().toBytes() }, operationResultReference)


      // We can't directly set the TTL here, so we queue an instruction to set the ttl in a bit
      updateTtlSink.emitNext(
         UpdateTtlEvent(key, expirationTime),
         Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(10))
      )
      return cachedTypedInstanceList
   }

   private fun collateDataSources(typedInstances: List<TypedInstance>): OperationResultReference? {
      val sources = typedInstances.map { typedInstance ->
         when (val dataSource = typedInstance.source) {
            is OperationResultReference -> dataSource
            else -> {
               logger.warn { "Data source of type ${dataSource::class.simpleName} is not cacheable." }
               UndefinedSource
            }
         }
      }.distinct()
      return if (sources.size == 1 && sources.single() is OperationResultReference) {
         sources.single() as OperationResultReference
      } else {
         null
      }
   }


   private fun createOperationResult(
      startTime: Long,
      message: OperationInvocationParamMessage,
      cacheKey: OperationCacheKey,
      resultSize: Int,
      dataSource: CachedOperationResultReference,
   ): OperationResult {
      val remoteCall = RemoteCall(
         remoteCallId = dataSource.cacheReadOperationId,
         service = CacheNames.cacheServiceName(connectionName).fqn(),
         address = connectionAddress,
         operation = CacheNames.cacheReadOperationName(message.operation.name),
         method = CacheNames.CACHE_READ_OPERATION_NAME,
         durationMs = Duration.ofNanos(System.nanoTime() - startTime).toMillis(),
         exchange = CacheExchange(
            connectionName,
            message.operation.name,
            cacheKey,
            CacheExchange.CacheOperationVerb.GET_CACHED_RESULT,
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
      return operationResult
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
data class CachedTypedInstanceList(
   val expiresAt: Long?,
   val instances: List<ByteArray>,
   val dataSource: DataSource
) {
   companion object {
      // Started out using hashcode, but was breaking between local compilation rounds - not sure why
      const val SERIALIZATION_ID = 10_000
   }

   constructor(
      expiresAt: Instant?,
      value: List<ByteArray>,
      dataSource: OperationResultReference?
   ) : this(expiresAt?.toEpochMilli(), value, dataSource ?: UndefinedSource)

   fun isExpired(clock: Clock): Boolean {
      return when (expiresAt) {
          null -> false
          else -> expiresAt < clock.instant().toEpochMilli()
      }
   }
}


object OperationParamCompactSerializer : CompactSerializer<OperationResult.OperationParam> {

   override fun getTypeName(): String = OperationResult.OperationParam::class.simpleName!!
   override fun getCompactClass(): Class<OperationResult.OperationParam> = OperationResult.OperationParam::class.java

   override fun read(reader: CompactReader): OperationResult.OperationParam {
      val paramName = reader.readString(OperationResult.OperationParam::parameterName.name)!!
      val value = reader.readString(OperationResult.OperationParam::value.name)!!
      return OperationResult.OperationParam(paramName, value)
   }

   override fun write(writer: CompactWriter, operationParam: OperationResult.OperationParam) {
      writer.writeString(OperationResult.OperationParam::parameterName.name, operationParam.parameterName)
      writer.writeString(OperationResult.OperationParam::value.name, operationParam.value?.toString() ?: "null")
   }

}

object QualifiedNameCompactSerializer : CompactSerializer<QualifiedName> {
   override fun read(reader: CompactReader): QualifiedName {
      val parameterizedName = reader.readString(QualifiedName::parameterizedName.name)!!
      return parameterizedName.fqn()
   }

   override fun getTypeName(): String = QualifiedName::class.simpleName!!
   override fun getCompactClass(): Class<QualifiedName> = QualifiedName::class.java

   override fun write(writer: CompactWriter, value: QualifiedName) {
      writer.writeString(QualifiedName::parameterizedName.name, value.parameterizedName)
   }
}


/***
 * Do not attempt to inject SchemaStore as when DistributedSchemaStoreClient (injected when vyne.schema.server.clustered=true)
 * which can be injected into IMap context (DistributedSchemaStoreClient has two IMap members which seems to be causing trouble for HZ run-time)
 * and we end up Hazelcast Serialisation errors when this is executed in an Orbital HazelcastInstance object (strangely it works fine with a HazelcastClientInstance)
 */
class ExpiringTypedInstanceCustomSerializer : StreamSerializer<CachedTypedInstanceList> {
   private val clock: Clock = Clock.systemUTC()
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun getTypeId(): Int = CachedTypedInstanceList.SERIALIZATION_ID

   override fun write(out: ObjectDataOutput, value: CachedTypedInstanceList) {
      StrategyPerformanceProfiler.profiled("Hazelcast serialize") {
         out.writeLong(value.expiresAt ?: -1L)
         val hasOperationResultReference = when (value.dataSource) {
            is UndefinedSource -> false
            is OperationResultReference -> true
            else -> {
               logger.warn { "Serializing data source of ${value.dataSource::class.simpleName} is not supported. Will be treated as an UndefinedSource" }
               false
            }
         }
         out.writeBoolean(hasOperationResultReference)
         if (hasOperationResultReference) {
            out.writeObject(value.dataSource)
         }

         out.writeInt(value.instances.count())
         value.instances.forEach { typedInstanceByteArray ->
            out.writeByteArray(typedInstanceByteArray)
         }
      }

   }

   override fun read(input: ObjectDataInput): CachedTypedInstanceList {
      val expirationDate = input.readLong()
      // If this is already expired, don't bother reading any further
      if (expirationDate != -1L && expirationDate < clock.instant().toEpochMilli()) {
         return CachedTypedInstanceList(expirationDate, emptyList(), UndefinedSource)
      }

      val hasDatasource = input.readBoolean()
      val dataSource = if (hasDatasource) {
         val originalOperationResult = input.readObject<OperationResultReference>()!!
         CachedOperationResultReference(
            originalOperationResult
         )
      } else UndefinedSource

      val typedInstanceByteArray = mutableListOf<ByteArray>()
      StrategyPerformanceProfiler.profiled("Hazelcast deserialize") {
         val listSize = input.readInt()
         for (i in 0 until listSize) {
            val bytes = input.readByteArray() ?: error("Expected to find a byteArray at index $i, but value was null")
            typedInstanceByteArray.add(bytes)
         }
      }
      return CachedTypedInstanceList(expirationDate, typedInstanceByteArray, dataSource)
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


private fun calculateTtlFromResults(
   resultValues: List<Pair<out Any, Instant?>>,
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

/**
 * A data source for objects returned from a cache read
 */
data class CachedOperationResultReference(
   val originalResult: OperationResultReference,

   // This is a bit back-to-front, but we actually end up
   // creating this - which will become the RemoteCallId
   // within the data source - that's because this
   // object is returned during deserialization
   val cacheReadOperationId: String = Ids.fastUuid()
) : DataSource {
   companion object {
      const val NAME: String = "Cached operation result"
   }

   override val name: String = NAME
   override val id: String = cacheReadOperationId
   override val failedAttempts: List<DataSource> = emptyList()
}
