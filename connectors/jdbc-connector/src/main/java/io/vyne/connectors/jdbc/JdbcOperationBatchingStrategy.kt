package io.vyne.connectors.jdbc

import com.google.common.cache.CacheBuilder
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.batch.OperationBatchingStrategy
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.selects.select
import mu.KotlinLogging
import java.time.Duration

private val logger = KotlinLogging.logger { }

class JdbcOperationBatchingStrategy(
   private val jdbcInvoker: JdbcInvoker,
   private val batchMetricCollector: BatchTraceCollector = object : BatchTraceCollector {
      override fun reportSqlBatchQuery(sqlQuery: String, parameterNameValueMap: Map<String, Any>) {
         logger.info { "batch sql => $sqlQuery" }
      }

   },
   private val batchSettings: BatchSettings = BatchSettings(MAX_SIZE, MAX_TIME)) : OperationBatchingStrategy {
   private val batchFlowCache = CacheBuilder.newBuilder()
      .removalListener<String, SendChannel<BatchedOperation>> { notification ->
         logger.info { "Caching operation invoker removing entry for ${notification.key} for reason ${notification.cause}" }
      }
      .build<String, SendChannel<BatchedOperation>>()

   override fun canBatch(
      service: Service,
      operation: RemoteOperation,
      schema: Schema,
      preferredParams: Set<TypedInstance>,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): Boolean {
      return service.hasMetadata(JdbcConnectorTaxi.Annotations.DatabaseOperation.NAME)
   }

   override suspend fun invokeInBatch(
      service: Service,
      operation: RemoteOperation,
      preferredParams: Set<TypedInstance>,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      schema: Schema,
      queryId: String?
   ): Flow<TypedInstance> {
      val cacheKey = generateCacheKey(service, operation, parameters)
      return callbackFlow {
         batchFlowCache.get(cacheKey) {
            batchChannel()
         }.send(
            BatchedOperation(
               service, operation, parameters, eventDispatcher,
               object : TypedInstanceSupplier {
                  override fun onNextValue(value: TypedInstance) {
                     logger.info { "result from batch =>  ${value.toRawObject()}" }
                     trySend(value)
                  }

               override fun onCompleted() {
                  channel.close()
               }

               override fun onError(throwable: Throwable) {
                  logger.error(throwable) { "Batch Query Failed"  }
                  cancel(CancellationException("JDBC Invoker Error", throwable))
               }
            }
         ))

         awaitClose {
            logger.trace { "closing the callback flow" }
         }
      }
   }

   private fun generateCacheKey(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>
   ): String {
      return """${service.name}:${operation.name}"""
   }


   private fun batchChannel(): Channel<BatchedOperation> {
      val channel = Channel<BatchedOperation>(Channel.BUFFERED)

      channel.consumeAsFlow()
         .asFlux()
         .bufferTimeout(batchSettings.batchSize, Duration.ofMillis(batchSettings.batchTimeoutInMsecs))
         .subscribe { batch ->
            try {
               jdbcInvoker.batchInvoke(batch.toList(), batchMetricCollector)
            } catch (e: Exception) {
               logger.error(e) { "error in batch invoking jdbc invoker" }
               batch.forEach { op -> op.consumer.onError(e) }
            }
         }

      return channel
   }


   /**
    * instead of below, we are using @see batchChannel() but keeping this function as a potential
    * solution if ever hit backpressure related problems ( see https://github.com/reactor/reactor-core/issues/1099 )
    * in bufferTimeout operator that we use in batchChannel() function.
    */
   private fun batchActor() = CoroutineScope(Dispatchers.IO).actor<BatchedOperation> {
      val batch = mutableListOf<BatchedOperation>()
      var deadline = 0L // deadline for sending this batch to DB
      while (true) {
         // when deadline is reached or size is exceeded, then force batch to DB
         val remainingTime = deadline - System.currentTimeMillis()
         if (batch.isNotEmpty() && remainingTime <= 0 || batch.size >= MAX_SIZE) {
            logger.info { "Executing the batch" }
            jdbcInvoker.batchInvoke(batch.toList(), batchMetricCollector)
            batch.clear()
            continue
         }
         // wait until items is received or timeout reached
         select<Unit> {
            // when received -> add to batch
            channel
               .consumeAsFlow()
               .asFlux()
               .bufferTimeout(100, Duration.ofMillis(500))
               .subscribe {
                  batch.addAll(it)
               }
            channel.onReceive {
               batch.add(it)
               // init deadline on first item added to batch
               if (batch.size == 1) deadline = System.currentTimeMillis() + MAX_TIME
            }
            // when timeout is reached just finish select, note: no timeout when batch is empty
            if (batch.isNotEmpty()) onTimeout(remainingTime) {}
         }
      }
   }

   companion object {
      const val MAX_SIZE = 18 // max number of data items in batch
      const val MAX_TIME = 500L // max time (in ms) to wait
   }

   data class BatchedOperation(val service: Service,
                               val operation: RemoteOperation,
                               val parameters: List<Pair<Parameter, TypedInstance>>,
                               val eventDispatcher: QueryContextEventDispatcher,
                               val consumer: TypedInstanceSupplier)

   interface TypedInstanceSupplier {
      fun onNextValue(value: TypedInstance)
      fun onCompleted()
      fun onError(throwable: Throwable)
   }
}

interface BatchTraceCollector {
   fun reportSqlBatchQuery(sqlQuery: String, parameterNameValueMap: Map<String, Any>)
}

data class BatchSettings(val batchSize: Int = 100, val batchTimeoutInMsecs: Long = 500L)


