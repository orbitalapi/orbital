package com.orbitalhq.connectors

import com.google.common.cache.CacheBuilder
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.Duration


private val logger = KotlinLogging.logger { }

class BatchWriteCacheProvider<TCacheData, TCallBackData> {
   // We are using a Guava cache rather than a ConcurrentHashMap here.
   // The reason for that is ConcurrentHashMap::getOrPut guarantees not to put the value into the map if the key is already there,
   // but the defaultValue function may be invoked even if the key is already in the map. We can't allow `defaultValue` to be invoked
   // multiple times here.
   private val batchDataCache = CacheBuilder
      .newBuilder()
      .build<String, BatchWriteCache<TCacheData, TCallBackData>>()

   /**
    * Creates a batch writer for the provided query id - if not already present.
    * The provided callback is responsible for writing the actual batch.
    * The callback will only be invoked by one of the batch emitters - the one that created the cache entry.
    */
   @OptIn(ExperimentalCoroutinesApi::class)
   fun forQueryId(
      queryId: String,
      batchSize: Int,
      batchTimeoutInMillis: Long,
      job: Job,
      writeOperation: (List<TCacheData>) -> Mono<TCallBackData>
   ): BatchWriteCache<TCacheData, TCallBackData> {
      /**
       * Hooks to dispose the subscription when the query completes or is cancelled.
       */
      job.parent?.invokeOnCompletion { removeBatchWriteCache(queryId) }
      return batchDataCache.get(queryId) {
         logger.info { "Creating the Batch Write Cache for query $queryId" }
         val sink = Sinks.many().multicast()
            .onBackpressureBuffer<Pair<TCacheData, Sinks.One<TCallBackData>>>()
         val subscription = sink.asFlux()
            .doOnEach { _ ->
               logger.debug { "Batch writer provider received item for queryId $queryId" }
            }
            .bufferTimeout(batchSize, Duration.ofMillis(batchTimeoutInMillis))
            .subscribe { batch ->
               val instancesToWrite = batch.map { it.first }
               val callbacks = batch.map { it.second }
               writeOperation(instancesToWrite).subscribe { operationResult ->
                  callbacks.forEach { it.tryEmitValue(operationResult) }
               }
            }
         BatchWriteCache(sink, batchSize, batchTimeoutInMillis, subscription, queryId)
      }
   }

   private fun removeBatchWriteCache(queryId: String) {
      logger.info { "Removing Batch Write Cache for query $queryId" }
      batchDataCache.getIfPresent(queryId)?.let { batchWriteCache ->
         batchWriteCache.dispose()
      }
      batchDataCache.invalidate(queryId)
   }
}


data class BatchWriteCache<TCacheData, TCallBackData>(
   private val sink: Sinks.Many<Pair<TCacheData, Sinks.One<TCallBackData>>>,
   private val batchSize: Int,
   private val batchTimeoutInMillis: Long,
   private val subscription: Disposable,
   private val queryId: String
): Disposable {
   /**
    * Emits the item to be persisted in the batch.
    * Returns a Mono<> that completes when this item has been persisted.
    */
   fun emit(item: TCacheData): Mono<TCallBackData> {
      val completionSink = Sinks.one<TCallBackData>()
      sink.emitNext(item to completionSink, RetryFailOnSerializeEmitHandler)
      return completionSink.asMono()
   }

   override fun dispose() {
      try {
         if (!subscription.isDisposed) {
            logger.info { "Disposing the Batch Write Cache for query $queryId" }
            subscription.dispose()
         }

      } catch (e: Exception) {
         logger.error(e) { "Error in disposing the Bach Write Cache for query $queryId "  }
      }
   }
}

