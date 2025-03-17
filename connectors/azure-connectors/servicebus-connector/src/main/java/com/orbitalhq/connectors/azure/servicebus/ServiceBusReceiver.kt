package com.orbitalhq.connectors.azure.servicebus

import arrow.core.Either
import arrow.core.right
import com.azure.messaging.servicebus.ServiceBusReceivedMessage
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.connectors.streamTypeOrType
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.MessageStreamExchange
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {  }
class ServiceBusReceiver(private val serviceBusConnectionFactory: ServiceBusConnectionFactory,
                         private val formatRegistry: FormatRegistry,
                         private val meterRegistry: MeterRegistry,
                         private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper
) {
    fun subscribeToTopic(
        connectionName: String,
        serviceBusTopicOperation: ServiceBusTaxi.Annotations.ServiceBusTopicSubscriptionOperation,
        service: Service,
        operation: RemoteOperation,
        schema: Schema,
        queryId: String,
        queryOptions: QueryOptions
    ): Flow<Either<StreamErrorMessage, TypedInstance>> {
        val dataSource = buildDataSource(service,
            operation,
            serviceBusConnectionFactory.serviceBusConnection(connectionName).endPointUrl,
            mapOf("topic" to serviceBusTopicOperation.topic, "subscription" to serviceBusTopicOperation.subscription),
            serviceBusTopicOperation.topic)

        val messageType = schema.type(streamTypeOrType(operation.returnType.taxiType))

       return serviceBusConnectionFactory
            .topicReceiver(connectionName, serviceBusTopicOperation.topic, serviceBusTopicOperation.subscription)
            .receiveMessages()
            .publishOn(Schedulers.boundedElastic())
            .doOnEach {
                meterRegistry.counter("orbital.connections.servicebus.$connectionName.topic.${serviceBusTopicOperation.topic}.subscription.${serviceBusTopicOperation.subscription}.messagesReceived")
                    .increment()
            }.errorOrTypedInstance(connectionName, messageType, schema, dataSource, "topic [${serviceBusTopicOperation.topic} / ${serviceBusTopicOperation.subscription}]")
    }

    fun subscribeToQueue(
        connectionName: String,
        serviceBusQueueOperation: ServiceBusTaxi.Annotations.ServiceBusQueueOperation,
        service: Service,
        operation: RemoteOperation,
        schema: Schema,
        queryId: String,
        queryOptions: QueryOptions
    ): Flow<Either<StreamErrorMessage, TypedInstance>> {
        val dataSource = buildDataSource(service,
            operation,
            serviceBusConnectionFactory.serviceBusConnection(connectionName).endPointUrl,
            mapOf("queue" to serviceBusQueueOperation.queue),
            serviceBusQueueOperation.queue)

        val messageType = schema.type(streamTypeOrType(operation.returnType.taxiType))

        return serviceBusConnectionFactory
            .queueReceiver(connectionName, serviceBusQueueOperation.queue)
            .receiveMessages()
            .publishOn(Schedulers.boundedElastic())
            .doOnEach {
                meterRegistry.counter("orbital.connections.servicebus.$connectionName.queue.${serviceBusQueueOperation.queue}.messagesReceived")
                    .increment()
            }.errorOrTypedInstance(connectionName, messageType, schema, dataSource, "queue [${serviceBusQueueOperation.queue}]")
    }


    private fun Flux<ServiceBusReceivedMessage>.errorOrTypedInstance(
        connectionName: String,
        messageType: Type,
        schema: Schema,
        dataSource: DataSource,
        operationDetails: String): Flow<Either<StreamErrorMessage, TypedInstance>> {
        return map { serviceBusMessage ->
            val messageValue = serviceBusMessage.body.toString()
            val typedInstanceOrError = try {
                Either.Right(
                    TypedInstance.from(
                        messageType,
                        messageValue,
                        schema,
                        formatSpecs = formatRegistry.formats,
                        source = dataSource)
                )

            } catch (e: Exception) {
                val errorMessage = StreamErrorMessage(
                    timestamp = Instant.now(),
                    exception = e,
                    message = e.message ?: e::class.simpleName!!,
                    typeName = messageType.paramaterizedName,
                    payload = messageValue
                )

                logger.info { "Failed to parse TypedInstance from kafka data for type => ${messageType.longDisplayName}  - error: ${errorMessage.message}" }
                Either.Left(errorMessage)
            }
            typedInstanceOrError

        }.asFlow().flowOn(Dispatchers.IO)
            .catch {

                logger.error(it.cause ?: it) { "Error in ServiceBus $operationDetails subscription for serviceBus connection $connectionName"  }
                // see the error handling notes for SharedFlow:
                // https://github.com/Kotlin/kotlinx.coroutines/issues/2034
                val errorMessage = "Error in ServiceBus connection: $connectionName, details: ${it.cause?.message}"
                this.emit(ErrorType.errorMessage(errorMessage, schema, dataSource).right())
            }
    }


    private fun buildDataSource(
        service: Service,
        operation: RemoteOperation,
        url: String,
        requestBody: Map<String, Any>,
        streamExchangeTopic: String
    ): DataSource {

        val remoteCall = RemoteCall(
            service = service.name,
            address = url,
            operation = operation.name,
            responseTypeName =operation.returnType.name,
            requestBody = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(requestBody),
            // What should we use for the duration?  Using zero, because I can't think of anything better
            durationMs = Duration.ZERO.toMillis(),
            timestamp = Instant.now(),
            responseMessageType = ResponseMessageType.EVENT,
            // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
            response = null,
            exchange = MessageStreamExchange(
                topic = streamExchangeTopic
            )
        )
        return OperationResultDataSourceWrapper(
            OperationResult.from(
                emptyList(),
                remoteCall
            )
        )
    }
}