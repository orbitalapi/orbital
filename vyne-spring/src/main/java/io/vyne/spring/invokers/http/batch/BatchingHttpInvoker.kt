package io.vyne.spring.invokers.http.batch

import com.google.common.cache.CacheBuilder
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.batch.OperationBatchingStrategy
import io.vyne.schemas.*
import io.vyne.spring.invokers.RestTemplateInvoker
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import mu.KotlinLogging
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import java.time.Duration

class BatchingHttpInvoker(
   private val httpInvoker: RestTemplateInvoker,
   private val batchSettings: BatchSettings = BatchSettings()
) : OperationBatchingStrategy {
   private val logger = KotlinLogging.logger {}
   private val batchFlowCache = CacheBuilder.newBuilder()
      .removalListener<Operation, SendChannel<BatchedOperation>> { notification ->
         logger.info { "Caching operation invoker removing entry for ${notification.key} for reason ${notification.cause}" }
      }
      .build<Operation, SendChannel<BatchedOperation>>()

   override fun canBatch(
      service: Service,
      operation: RemoteOperation,
      schema: Schema,
      preferredParams: Set<TypedInstance>,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): Boolean {
      return findBatchingOperation(operation, schema, service, preferredParams, providedParamValues) != null
   }

   private val batchingOperationStrategies =
      listOf(CollectionOfEntitiesStrategy(), BatchRequestIsMappableCollectionOfSingleRequestResponseObjectsStrategy())

   private fun findBatchingOperation(
      operation: RemoteOperation,
      schema: Schema,
      service: Service,
      preferredParams: Set<TypedInstance>,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): BatchingOperationCandidate? {
      return batchingOperationStrategies
         .asSequence()
         .mapNotNull { strategy ->
            strategy.findBatchingCandidate(
               operation,
               schema,
               service,
               preferredParams,
               providedParamValues
            )
         }
         .firstOrNull()

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
      val sink = Sinks.many().unicast().onBackpressureBuffer<TypedInstance>()
      batchFlowCache.get(operation as Operation) {
         val batchingParameters =
            findBatchingOperation(operation, schema, service, preferredParams, parameters)
               ?: error("Expected to find a batching operation!")
         batchChannel(
            batchingParameters.service,
            batchingParameters.operation,
            batchingParameters.accumulator,
            batchingParameters.resultMatchingStrategy,
            eventDispatcher,
         )
      }.send(
         BatchedOperation(
            service, operation, parameters, eventDispatcher, sink, schema
         )
      )
      return sink.asFlux().asFlow()
   }

   private fun batchChannel(
      service: Service,
      batchedOperationHandler: Operation,
      parameterAccumulatorStrategy: ParameterAccumulatorStrategy,
      resultMatchingStrategy: ResultMatchingStrategy,
      eventDispatcher: QueryContextEventDispatcher,
   ): Channel<BatchedOperation> {
      val channel = Channel<BatchedOperation>(Channel.BUFFERED)

      CoroutineScope(Dispatchers.IO).launch {
         channel.consumeAsFlow()
            .asFlux() // convert to a flux, to access bufferTimeout(), which there's currently no analogous Flow operation
            .bufferTimeout(batchSettings.batchSize, batchSettings.batchTimeout)
            .asFlow()
            .collect { batch ->
               try {
                  resultMatchingStrategy.beforeSend(batch)
                  val requestParam = buildParameterForBatch(batch, parameterAccumulatorStrategy)
                  val httpResult = httpInvoker.invoke(
                     service, batchedOperationHandler, requestParam, eventDispatcher
                  )
                  httpResult
                     .onCompletion {
                        // TODO : If there was no match, emit a typedNull
                        batch.forEach { operation -> operation.tryEmitComplete() }
                     }
                     .collect { result ->
                        resultMatchingStrategy.findOriginatingOperation(batch, result as TypedObject)
                           .forEach { (operation, result) ->
                              operation.emit(result)
                           }

                     }
               } catch (e: Exception) {
                  logger.error(e) { "error in batch invoking Http Batching invoker" }
                  batch.forEach { op -> op.tryEmitError(e) }
               }
            }
      }


      return channel
   }

   private fun buildParameterForBatch(
      batch: List<BatchedOperation>,
      accumulator: ParameterAccumulatorStrategy
   ): List<Pair<Parameter, TypedInstance>> {
      // TODO  :Ideally, this would just be a call out to Vyne to construct the param --
      // or at least re-use the code in ParameterFactory.
      // However, ParameterFactory requires access to a query context, and at this point, we have multiple.
      // Also, ParameterFactory does a lot of searching from the graph to constructor the object
      // However, right now there's a very limited set of scenarios we support batching for,
      // so we've extracted those to specific strategies, which we invoke here.
      val allParams = batch.flatMap { it.parameters }
      return accumulator.build(allParams)
   }


}

data class BatchingOperationCandidate(
   val service: Service,
   val operation: Operation,
   val accumulator: ParameterAccumulatorStrategy,
   val resultMatchingStrategy: ResultMatchingStrategy
)

data class BatchSettings(val batchSize: Int = 100, val batchTimeout: Duration = Duration.ofMillis(250))
data class BatchedOperation(
   val service: Service,
   val operation: RemoteOperation,
   val parameters: List<Pair<Parameter, TypedInstance>>,
   val eventDispatcher: QueryContextEventDispatcher,
   private val consumer: Sinks.Many<TypedInstance>,
   val schema: Schema
) : Many<TypedInstance> by consumer {
   var hasHadResult: Boolean = false
      private set;


   fun emit(instance: TypedInstance) {
      consumer.emitNext(instance, Sinks.EmitFailureHandler.FAIL_FAST)
      hasHadResult = true
   }
}

