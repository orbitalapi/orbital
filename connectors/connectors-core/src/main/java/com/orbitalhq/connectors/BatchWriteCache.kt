package com.orbitalhq.connectors

import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.utils.Ids
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap


private val logger = KotlinLogging.logger { }

class BatchWriteCacheProvider {
   private val batchDataCache = ConcurrentHashMap<String, BatchWriteCache>()

   /**
    * Creates a batch writer for the provided query id - if not already present.
    *
    * The provided callback is responsible for writing the actual batch.
    * The callback will only be invoked by one of the batch emitters - the one that created the cache entry.
    */
   fun forQueryId(
      queryId: String,
      batchSize: Int,
      batchTimeoutInMillis: Long,
      writeOperation: (List<TypedInstance>) -> Mono<OperationResultReference>
   ): BatchWriteCache {
      return batchDataCache.getOrPut(queryId) {

         val sink = Sinks.many().multicast()
            .onBackpressureBuffer<Pair<TypedInstance, Sinks.One<OperationResultReference>>>()
         sink.asFlux()
            .doOnEach { next ->
               logger.debug { "Batch writer provider received item for queryId $queryId" }
            }
            .bufferTimeout(batchSize, Duration.ofMillis(batchTimeoutInMillis))
            .subscribe { batch -> //batch ->

               val instancesToWrite = batch.map { it.first }
               val callbacks = batch.map { it.second }
               writeOperation(instancesToWrite).subscribe { operationResult ->
                  callbacks.forEach { it.tryEmitValue(operationResult) }
               }
            }
         BatchWriteCache(sink, batchSize, batchTimeoutInMillis)
      }
   }

}


data class BatchWriteCache(
   private val sink: Sinks.Many<Pair<TypedInstance, Sinks.One<OperationResultReference>>>,
   private val batchSize: Int,
   private val batchTimeoutInMillis: Long,
) {
   /**
    * Emits the item to be persisted in the batch.
    * Returns a Mono<> that completes when this item has been persisted.
    */
   fun emit(item: TypedInstance): Mono<OperationResultReference> {
      val completionSink = Sinks.one<OperationResultReference>()
      sink.emitNext(item to completionSink, RetryFailOnSerializeEmitHandler)
      return completionSink.asMono()
   }
}

