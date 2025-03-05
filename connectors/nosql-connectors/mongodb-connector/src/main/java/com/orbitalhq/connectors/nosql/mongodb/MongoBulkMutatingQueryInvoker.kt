package com.orbitalhq.connectors.nosql.mongodb

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.orbitalhq.connectors.BatchWriteCacheProvider
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.fqn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger { }

class MongoBulkMutatingQueryInvoker(
   connectionFactory: MongoConnectionFactory,
   schemaProvider: SchemaProvider,
   private val batchWriteCacheProvider: BatchWriteCacheProvider<TypedInstance, OperationResultReference>
) : MongoBaseInvoker(connectionFactory, schemaProvider) {

   private val counter = AtomicInteger(0)
   init {
       logger.debug { "New MongoBulkMutatingQueryInvoker created" }
   }
   suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      batchAttribute: MongoConnector.Annotations.BatchAttribute

   ): Flow<TypedInstance> {
      logger.debug { "Received item ${counter.incrementAndGet()} into bulk mutator" }
      require(operation.parameters.size == 1) { "Operations annotated with ${MongoConnector.Annotations.UpsertOperationAnnotationName} should accept exactly one type" }
      val inputType = operation.parameters.single().type.let { type -> type.collectionType ?: type }
      require(inputType.hasMetadata(MongoConnector.Annotations.Collection.NAME.fqn()))
      { "The input type into an ${MongoConnector.Annotations.UpsertOperationAnnotationName} operation should be a type with a ${MongoConnector.Annotations.Collection.NAME.fqn()} annotation" }

      // Validate there's a single parameter.
      // We don't hold a reference to the parameter, as it's not used (see below)
      parameters.singleOrNull()
         ?: error("Expected a single parameter, but received ${parameters.size}")
      val collectionName = inputType.taxiType.collectionNameOrTypeName()

      val (connectionConfig, reactiveMongoTemplate) = getConnectionConfigAndTemplate(service)
      val recordToWrite = parameters[0].second

      val batchWriteCache = batchWriteCacheProvider.forQueryId(
         queryId,
         batchAttribute.batchSize,
         batchAttribute.batchDurationInMillis,
         currentCoroutineContext().job
      ) { items ->
         logger.info { "Batch update triggered with ${items.size} items" }
         doBulkInsert(reactiveMongoTemplate, collectionName, items)
            .elapsed()
            .map { durationAndData ->
               val duration = durationAndData.t1
               val upsertResult = durationAndData.t2
               logger.info { "Mongo Upsert call completed in ${duration}ms. Total batch size was ${items.size}, result was ${upsertResult.insertedCount} inserted, ${upsertResult.modifiedCount} modified, ${upsertResult.deletedCount} deleted" }

               /**
                * Note that we're passing an empty list to parameters argument of buildOperationResult.
                * as passing the full batch of items ramps up the heap usage when we process large number of items.
                * The result that's returned here is used for lineage purposes,
                * the call is already completed.
                * TODO: Investigate why items are not GC'ed properly during execution"
                */
               val emptyParametersList = listOf<TypedInstance>()
               val operationResult = buildOperationResult(
                  service,
                  operation,
                  emptyParametersList,
                  "Upsert ${items.size} items to collection $collectionName",
                  connectionConfig.connectionString.hosts.joinToString(),
                  java.time.Duration.ofMillis(duration),
                  recordCount = items.size,
               )
               eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
               operationResult.asOperationReferenceDataSource()
            }
      }



      return batchWriteCache.emit(recordToWrite)
         .map { operationResult ->
            DataSourceUpdater.update(recordToWrite, operationResult)
         }
         .asFlow()
         .flowOn(Dispatchers.IO)
   }

   private fun doBulkInsert(
      reactiveMongoTemplate: ReactiveMongoTemplate,
      collectionName: String,
      items: List<TypedInstance>
   ): Mono<BulkWriteResult> {
      return reactiveMongoTemplate.getCollection(collectionName).flatMap { mongoCollection ->
         val writeModels = items.map { recordToWrite ->
            val documentMap = typedInstanceToMap(recordToWrite)
            val upsertDefinition = MongoCriteriaGenerator.bulkUpsertFor(recordToWrite, documentMap)
            if (upsertDefinition == null) {
               InsertOneModel(Document(documentMap))
            } else {
               val documentQuery = upsertDefinition.first.queryObject
               val documentUpdate = upsertDefinition.second.updateObject
               /**
                * With replaceOne() you can only replace the entire document, while updateOne() allows for updating fields.
                */
               UpdateOneModel(documentQuery, documentUpdate, UpdateOptions().upsert(true))
            }
         }

        Mono.from(
            mongoCollection.bulkWrite(writeModels)
         ).doOnError { t ->
           logger.error(t) { "error performing bulk write for $collectionName" }
        }
      }
   }
}

/**
 * The difference between insert and save operations is that a save operation performs an insert if the object is not already present.
 * When inserting or saving, if the Id property is not set, the assumption is that its value will be auto-generated by the database.
 * Consequently, for auto-generation of an ObjectId to succeed,
 * the type of the Id property or field in your class must be a String, an ObjectId, or a BigInteger.
 */
