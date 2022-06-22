package io.vyne.spring.invokers.http

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

   override fun canBatch(service: Service, operation: RemoteOperation, schema: Schema): Boolean {
      return findBatchingOperation(operation, schema, service) != null


   }

   private fun findBatchingOperation(
      operation: RemoteOperation,
      schema: Schema,
      service: Service
   ): BatchingOperationCandidate? {
      // We don't support QueryOperations round these parts...
      if (operation is QueryOperation) {
         return null
      }
      val returnType = operation.returnType
      val idFields = returnType.getAttributesWithAnnotation("Id".fqn())
      if (idFields.isEmpty()) {
         return null
      }

      // Note: No real reason that this couldn't be supported, but needs a bit of thought.
      if (idFields.size > 1) {
         logger.info { "Operation ${operation.qualifiedName.longDisplayName} is not batchable as it's return type has a composite key, which is not currently supported" }
         return null
      }

      val modelIdField = idFields.values.single()
      val idTypeAsArrayType = schema.type(modelIdField.type).asArrayType()


      val operationsReturningCollection = service.operations
         .filter { it.returnType == operation.returnType.asArrayType() }
         .mapNotNull { batchingCandidate ->
            val paramAccumulator = inputsIndicateBatchingLookup(
               batchingCandidate,
               idTypeAsArrayType,
               schema
            )
            if (paramAccumulator == null) {
               null
            } else {
               batchingCandidate to paramAccumulator
            }
         }
      // Bail early
      return when {
         operationsReturningCollection.isEmpty() -> null
         operationsReturningCollection.size > 1 -> {
            logger.info {
               "Multiple ambiguous operations are batching candidates for ${operation.qualifiedName}, so not batching.  Matching candidates: ${
                  operationsReturningCollection.map { it.first }.joinToString { it.qualifiedName.longDisplayName }
               }"
            }
            null
         }
         else -> {
            val (batchingOperation, accumulator) = operationsReturningCollection.single()
            BatchingOperationCandidate(service, batchingOperation, accumulator, schema.type(modelIdField.type))
         }
      }
   }

   /**
    * Returns true if EITHER:
    *  - operation takes an input that is a collection of Ids.
    *  - OR operation takes an input model that that has a single param, which is a collection of ids.
    */
   private fun inputsIndicateBatchingLookup(
      batchingCandidate: RemoteOperation,
      idTypeAsArrayType: Type,
      schema: Schema
   ): ParameterAccumulatorStrategy? {
      if (batchingCandidate.parameters.size != 1) {
         return null
      }


      val singleInputType = batchingCandidate.parameters.single().type
      return when {
         singleInputType == idTypeAsArrayType -> AccumulateAsArray(batchingCandidate.parameters.single())
         singleInputType.attributes.size == 1 && singleInputType.attributes.values.single().type == idTypeAsArrayType.qualifiedName -> AccumulateAsArrayAttributeOnRequest(
            batchingCandidate.parameters.single(),
            schema
         )
         else -> null
      }
   }

   override suspend fun invokeInBatch(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      schema: Schema,
      queryId: String?
   ): Flow<TypedInstance> {
      val sink = Sinks.many().unicast().onBackpressureBuffer<TypedInstance>()
      batchFlowCache.get(operation as Operation) {
         val batchingParameters =
            findBatchingOperation(operation, schema, service) ?: error("Expected to find a batching operation!")
         batchChannel(
            batchingParameters.service,
            batchingParameters.operation,
            batchingParameters.accumulator,
            eventDispatcher,
            batchingParameters.idFieldType
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
      eventDispatcher: QueryContextEventDispatcher,
      idFieldType: Type
   ): Channel<BatchedOperation> {
      val channel = Channel<BatchedOperation>(Channel.BUFFERED)

      CoroutineScope(Dispatchers.IO).launch {
         channel.consumeAsFlow()
            .asFlux() // convert to a flux, to access bufferTimeout(), which there's currently no analogous Flow operation
            .bufferTimeout(batchSettings.batchSize, batchSettings.batchTimeout)
            .asFlow()
            .collect { batch ->
               try {
                  val requestParam = buildParameterForBatch(batch, parameterAccumulatorStrategy)
                  val originatingOperationById: Map<TypedInstance, BatchedOperation> =
                     groupByIdField(batch, idFieldType)
                  val httpResult = httpInvoker.invoke(
                     service, batchedOperationHandler, requestParam, eventDispatcher
                  )
                  httpResult
                     .onCompletion {
                        // TODO : If there was no match, emit a typedNull
                        batch.forEach { operation -> operation.tryEmitComplete() }
                     }
                     .collect { result ->
                        val originatingOperation =
                           findOriginatingOperation(originatingOperationById, result as TypedObject, idFieldType)
                        originatingOperation.emit(result)
                     }
               } catch (e: Exception) {
                  logger.error(e) { "error in batch invoking jdbc invoker" }
                  batch.forEach { op -> op.tryEmitError(e) }
               }
            }
      }


      return channel
   }

   private fun groupByIdField(batch: List<BatchedOperation>, idFieldType: Type): Map<TypedInstance, BatchedOperation> {
      return batch.associateBy { operation ->
         val idParamPair = operation.parameters.single { it.first.type == idFieldType }
         idParamPair.second
      }
   }

   private fun findOriginatingOperation(
      originatingOperations: Map<TypedInstance, BatchedOperation>,
      result: TypedObject,
      idFieldType: Type
   ): BatchedOperation {
      val idValue = result.getAttributeIdentifiedByType(idFieldType)
      return originatingOperations[idValue]
         ?: error("Could not find originating request for ${result.type.longDisplayName} with id ${idValue.value?.toString()}")
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

   private data class BatchingOperationCandidate(
      val service: Service,
      val operation: Operation,
      val accumulator: ParameterAccumulatorStrategy,
      val idFieldType: Type
   )
}


data class BatchSettings(val batchSize: Int = 100, val batchTimeout: Duration = Duration.ofMillis(500))
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

