package io.vyne.connectors.aws.dynamodb

import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.models.DataSourceUpdater
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

class DynamoDbUpsertInvoker(
    private val connectionRegistry: AwsConnectionRegistry,
    private val schemaProvider: SchemaProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseDynamoInvoker(connectionRegistry) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val queryBuilder = DynamoDbRequestBuilder()

    fun invoke(
        service: Service,
        operation: RemoteOperation,
        parameters: List<Pair<Parameter, TypedInstance>>,
        eventDispatcher: QueryContextEventDispatcher,
        queryId: String
    ): Flow<TypedInstance> {
        val schema = schemaProvider.schema
        val recordToWrite = parameters[0].second
        val request = queryBuilder.buildPut(schemaProvider.schema, recordToWrite)

        val (client, awsConfig) = buildClient(service, operation)
        return Mono.fromFuture(executeRequest(request, client))
            .publishOn(Schedulers.boundedElastic())
            .elapsed()
            .flatMapMany<TypedInstance> { responsePair ->
                val duration = responsePair.t1
                val response = responsePair.t2
                val count = 1
                val remoteCall = buildRemoteCall(service, awsConfig, operation, request, duration, count)
                val operationResult = OperationResult.fromTypedInstances(parameters.map { it.second }, remoteCall)
                eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
                val writeResultValue =
                    DataSourceUpdater.update(recordToWrite, operationResult.asOperationReferenceDataSource())
                Flux.fromIterable(listOf(writeResultValue))
            }.asFlow().flowOn(dispatcher)
    }
}
