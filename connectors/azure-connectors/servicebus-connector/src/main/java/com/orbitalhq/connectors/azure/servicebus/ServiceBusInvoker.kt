package com.orbitalhq.connectors.azure.servicebus

import arrow.core.Either
import com.orbitalhq.connectors.StreamErrorMessage
import com.orbitalhq.connectors.StreamErrorPublisher
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTaxi.Annotations.ServiceBusService.Companion.ConnectionAttribute
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.QueryContextSchemaProvider
import com.orbitalhq.query.connectors.OperationCachingBehaviour
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import lang.taxi.services.OperationScope
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
class ServiceBusInvoker(
    private val schemaProvider: SchemaProvider,
    private val serviceBusPublisher: ServiceBusPublisher,
    private val serviceBusReceiver: ServiceBusReceiver,
    private val streamErrorPublisher: StreamErrorPublisher
): OperationInvoker {
    override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
        return service.hasMetadata(ServiceBusTaxi.Annotations.ServiceBusService.NAME) &&
                (operation.hasMetadata(ServiceBusTaxi.Annotations.ServiceBusTopicPublicationOperation.NAME) ||
                        operation.hasMetadata(ServiceBusTaxi.Annotations.ServiceBusTopicSubscriptionOperation.NAME) ||
                        operation.hasMetadata(ServiceBusTaxi.Annotations.ServiceBusQueueOperation.NAME))
    }

    override suspend fun invoke(
        service: Service,
        operation: RemoteOperation,
        parameters: List<Pair<Parameter, TypedInstance>>,
        eventDispatcher: QueryContextEventDispatcher,
        queryId: String,
        queryOptions: QueryOptions
    ): Flow<TypedInstance> {
        val connectionName = service.firstMetadata(ServiceBusTaxi.Annotations.ServiceBusService.NAME).params[ConnectionAttribute] as String
        val serviceBusQueueOperation = operation.firstMetadataOrNull(ServiceBusTaxi.Annotations.ServiceBusQueueOperation.NAME)
            ?.let { ServiceBusTaxi.Annotations.ServiceBusQueueOperation.from(it) }
        val serviceBusTopicSubscriptionOperation = operation.firstMetadataOrNull(ServiceBusTaxi.Annotations.ServiceBusTopicSubscriptionOperation.NAME)
            ?.let { ServiceBusTaxi.Annotations.ServiceBusTopicSubscriptionOperation.from(it) }
        val serviceBusTopicPublicationOperation = operation.firstMetadataOrNull(ServiceBusTaxi.Annotations.ServiceBusTopicPublicationOperation.NAME)
            ?.let { ServiceBusTaxi.Annotations.ServiceBusTopicPublicationOperation.from(it) }

       if (serviceBusQueueOperation != null && serviceBusTopicSubscriptionOperation != null && serviceBusTopicPublicationOperation != null) {
           IllegalArgumentException("${operation.name} in service ${service.name} has " +
                   "both @${ServiceBusTaxi.Annotations.ServiceBusQueueOperation.NAME} and @${ServiceBusTaxi.Annotations.ServiceBusTopicSubscriptionOperation.NAME} and @${ServiceBusTaxi.Annotations.ServiceBusTopicPublicationOperation.NAME}")
       }

       return when {
           operation.operationType == OperationScope.MUTATION && serviceBusTopicPublicationOperation != null && serviceBusTopicPublicationOperation.isBatch ->
               this.serviceBusPublisher.batchPublishToTopic(service, operation, queryId, connectionName, serviceBusTopicPublicationOperation,parameters.single().second, schemaProvider.schema)
            operation.operationType == OperationScope.MUTATION && serviceBusTopicPublicationOperation != null -> publishToTopic(connectionName, serviceBusTopicPublicationOperation, service, operation, eventDispatcher, queryId, parameters)
           operation.operationType == OperationScope.MUTATION && serviceBusQueueOperation != null && serviceBusQueueOperation.isBatch->
               this.serviceBusPublisher.batchPublishToQueue(service, operation, queryId, connectionName, serviceBusQueueOperation,parameters.single().second, schemaProvider.schema)
            operation.operationType == OperationScope.MUTATION && serviceBusQueueOperation != null -> publishToQueue(connectionName, serviceBusQueueOperation, service, operation, eventDispatcher, queryId, parameters)
            operation.operationType == OperationScope.READ_ONLY && serviceBusQueueOperation != null ->
                serviceBusReceiver.subscribeToQueue(connectionName, serviceBusQueueOperation, service, operation, schemaProvider.schema, queryId, queryOptions )
                    .serviceBusSubscriber(eventDispatcher, queryId)
            operation.operationType == OperationScope.READ_ONLY && serviceBusTopicSubscriptionOperation != null ->
                serviceBusReceiver.subscribeToTopic(connectionName, serviceBusTopicSubscriptionOperation, service, operation, schemaProvider.schema, queryId, queryOptions )
                    .serviceBusSubscriber(eventDispatcher, queryId)
            else -> throw IllegalStateException("${operation.name} in service ${service.name} has invalid configuration for invocation.")
        }
    }

    private fun Flow<Either<StreamErrorMessage, TypedInstance>>.serviceBusSubscriber(
        eventDispatcher: QueryContextEventDispatcher,
        queryId: String,
    ): Flow<TypedInstance> {
       return mapNotNull { errorOrInstance ->
            when (errorOrInstance) {
                is Either.Right -> {
                    val instance = errorOrInstance.value
                    val dataSource = instance.source
                    require(dataSource is OperationResultDataSourceWrapper) { "Expected OperationResultDataSourceWrapper as the datasource, found ${dataSource::class.simpleName}" }
                    eventDispatcher.reportRemoteOperationInvoked(dataSource.operationResult, queryId)

                    DataSourceUpdater.update(instance, dataSource.operationResultReferenceSource)
                }

                is Either.Left -> {
                    streamErrorPublisher.onError(queryId, errorOrInstance.value)
                    null
                }
            }
        }

    }

    private fun publishToQueue(
        connectionName: String,
        serviceBusQueueOperation: ServiceBusTaxi.Annotations.ServiceBusQueueOperation,
        service: Service,
        operation: RemoteOperation,
        eventDispatcher: QueryContextEventDispatcher,
        queryId: String,
        parameters: List<Pair<Parameter, TypedInstance>>
    ): Flow<TypedInstance> {
        require(parameters.size == 1) { "Expected a single parameter (the message to publish), but found ${parameters.size}" }
        require(eventDispatcher is QueryContextSchemaProvider) { "EventDispatcher is not a QueryContext, Need a way to access the schema " }
        val schema = eventDispatcher.schema
        val messagePayload = parameters.single().second
        return serviceBusPublisher.publishToQueue(
            connectionName,
            serviceBusQueueOperation,
            messagePayload,
            schema
        )
    }

    override fun getCachingBehaviour(service: Service, operation: RemoteOperation): OperationCachingBehaviour {
        return OperationCachingBehaviour.NO_CACHE
    }

    private fun publishToTopic(
        connectionName: String,
        serviceBusTopicOperation: ServiceBusTaxi.Annotations.ServiceBusTopicPublicationOperation,
        service: Service,
        operation: RemoteOperation,
        eventDispatcher: QueryContextEventDispatcher,
        queryId: String,
        parameters: List<Pair<Parameter, TypedInstance>>
    ): Flow<TypedInstance> {
        require(parameters.size == 1) { "Expected a single parameter (the message to publish), but found ${parameters.size}" }
        require(eventDispatcher is QueryContextSchemaProvider) { "EventDispatcher is not a QueryContext, Need a way to access the schema " }
        val schema = eventDispatcher.schema
        val messagePayload = parameters.single().second
        return serviceBusPublisher.publishToTopic(
            connectionName,
            serviceBusTopicOperation,
            messagePayload,
            schema
        )
    }
}