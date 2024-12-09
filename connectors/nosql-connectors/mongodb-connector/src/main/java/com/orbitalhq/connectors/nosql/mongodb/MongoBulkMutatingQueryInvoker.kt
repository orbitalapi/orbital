package com.orbitalhq.connectors.nosql.mongodb

import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.orbitalhq.connectors.BatchWriteCacheProvider
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.fqn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger { }
class MongoBulkMutatingQueryInvoker(
    connectionFactory: MongoConnectionFactory,
    schemaProvider: SchemaProvider,
    private val batchWriteCacheProvider: BatchWriteCacheProvider
): MongoBaseInvoker(connectionFactory, schemaProvider) {

    suspend fun invoke(
        service: Service,
        operation: RemoteOperation,
        parameters: List<Pair<Parameter, TypedInstance>>,
        eventDispatcher: QueryContextEventDispatcher,
        queryId: String,
        batchAttribute: MongoConnector.Annotations.BatchAttribute

    ): Flow<TypedInstance> {
        require(operation.parameters.size == 1) { "Operations annotated with ${MongoConnector.Annotations.UpsertOperationAnnotationName} should accept exactly one type" }
        val inputType = operation.parameters.single().type.let { type -> type.collectionType ?: type }
        require(inputType.hasMetadata(MongoConnector.Annotations.Collection.NAME.fqn()))
        { "The input type into an ${MongoConnector.Annotations.UpsertOperationAnnotationName} operation should be a type with a ${MongoConnector.Annotations.Collection.NAME.fqn()} annotation" }

        val (param, input) = parameters.singleOrNull()
            ?: error("Expected a single parameter, but received ${parameters.size}")
        val collectionName = inputType.taxiType.collectionNameOrTypeName()

        val (connectionConfig, reactiveMongoTemplate) = getConnectionConfigAndTemplate(service)
        val recordToWrite = parameters[0].second
        val documentMap = typedInstanceToMap(recordToWrite)

        val batchWriter = batchWriteCacheProvider.withBatchSize(queryId, batchAttribute.batchSize, batchAttribute.batchDurationInMillis) { items ->
            bulkOpsCallback(reactiveMongoTemplate, collectionName, items)
        }

        return batchWriter.emit(recordToWrite)
            .elapsed()
            .map { durationAndData ->
                val duration = durationAndData.t1
                logger.info { "Mongo Upsert call completed in ${duration}ms "}
                val data = durationAndData.t2
                val operationResult = buildOperationResult(
                    service,
                    operation,
                    listOf( input),
                    "upsert",
                    connectionConfig.connectionString.hosts.joinToString(),
                    java.time.Duration.ofMillis(duration),
                    recordCount = 1
                )
                eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
                DataSourceUpdater.update(data, operationResult.asOperationReferenceDataSource())
            }.asFlow().onCompletion {
                logger.warn { "completed......" }
            }
    }

    private fun bulkOpsCallback(
        reactiveMongoTemplate: ReactiveMongoTemplate,
        collectionName: String,
        items: List<Pair<TypedInstance, FluxSink<TypedInstance>>>
                                ): Mono<Any> {
        return reactiveMongoTemplate.getCollection(collectionName).flatMap { mongoCollection ->
            val writeModels = items.map { item ->
                val recordToWrite = item.first
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
            ).onErrorResume { t ->
                logger.error(t) { "error performing bulk write for $collectionName" }
                items.forEach { it.second.error(t) }
                Mono.empty()
            }
                .map { bulkWriteResult ->
                items.forEach {
                    it.second.next(it.first)
                    it.second.complete()
                }
                bulkWriteResult
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
