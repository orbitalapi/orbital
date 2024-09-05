package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.BatchWriteCacheProvider
import com.orbitalhq.connectors.nosql.mongodb.MongoConnector.Annotations.BatchSizeAttributeName
import com.orbitalhq.connectors.nosql.mongodb.MongoConnector.Annotations.batchDurationAttributeName
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import lang.taxi.services.OperationScope
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class MongoDbInvoker(
    connectionFactory: MongoConnectionFactory,
    schemaProvider: SchemaProvider
) : OperationInvoker {

    companion object {
        init {
            MongoConnector.registerConnectionUsage()
        }
    }
    private val batchWriteCacheProvider = BatchWriteCacheProvider()
    private val readOnlyInvoker = MongoReadOnlyQueryInvoker(connectionFactory, schemaProvider)
    private val upsertInvoker = MongoMutatingQueryInvoker(connectionFactory, schemaProvider)
    private val bulkUpsertInvoker =MongoBulkMutatingQueryInvoker(connectionFactory, schemaProvider, batchWriteCacheProvider)
    override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
        return service.hasMetadata(MongoConnector.Annotations.MongoOperation.NAME)
    }

    override suspend fun invoke(
        service: Service,
        operation: RemoteOperation,
        parameters: List<Pair<Parameter, TypedInstance>>,
        eventDispatcher: QueryContextEventDispatcher,
        queryId: String,
        queryOptions: QueryOptions
    ): Flow<TypedInstance> {
        return when {
            operation.operationType == OperationScope.READ_ONLY -> readOnlyInvoker.invoke(
                service,
                operation,
                parameters,
                eventDispatcher,
                queryId,
                queryOptions
            )

            operation.operationType == OperationScope.MUTATION && operation.hasMetadata(MongoConnector.Annotations.UpsertOperationAnnotationName.parameterizedName) && isBatchUpsert(operation) -> bulkUpsertInvoker.invoke(
                service,
                operation,
                parameters,
                eventDispatcher,
                queryId,
                batchAttribute(operation)
            )

            operation.operationType == OperationScope.MUTATION && operation.hasMetadata(MongoConnector.Annotations.UpsertOperationAnnotationName.parameterizedName) && !isBatchUpsert(operation) -> upsertInvoker.invoke(
                service,
                operation,
                parameters,
                eventDispatcher,
                queryId,
                queryOptions
            )

            else -> error("Unhandled Mongo Operation type: ${operation.qualifiedName.parameterizedName}")
        }
    }

    private fun isBatchUpsert(operation: RemoteOperation): Boolean {
        val isBatch =  operation.firstMetadata(MongoConnector.Annotations.UpsertOperationAnnotationName.parameterizedName).params.containsKey(BatchSizeAttributeName) &&
                operation.firstMetadata(MongoConnector.Annotations.UpsertOperationAnnotationName.parameterizedName).params.containsKey(batchDurationAttributeName)
        return isBatch
    }

    private fun batchAttribute(operation: RemoteOperation): MongoConnector.Annotations.BatchAttribute {
        return MongoConnector.Annotations.BatchAttribute(
            operation.firstMetadata(MongoConnector.Annotations.UpsertOperationAnnotationName.parameterizedName).params[BatchSizeAttributeName] as Int,
            operation.firstMetadata(MongoConnector.Annotations.UpsertOperationAnnotationName.parameterizedName).params[batchDurationAttributeName].let { (it as Int).toLong() }
        )

    }
}
