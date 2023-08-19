package io.vyne.connectors.aws.dynamodb

import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.configureWithExplicitValuesIfProvided
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.query.*
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.*
import software.amazon.awssdk.services.dynamodb.model.QueryResponse
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.CompletableFuture

class DynamoDbQueryInvoker(
    private val connectionRegistry: AwsConnectionRegistry,
    private val schemaProvider: SchemaProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val queryBuilder = DynamoDbQueryBuilder()

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    suspend fun invoke(
        service: Service,
        operation: RemoteOperation,
        parameters: List<Pair<Parameter, TypedInstance>>,
        eventDispatcher: QueryContextEventDispatcher,
        queryId: String
    ): Flow<TypedInstance> {
        val schema = schemaProvider.schema
        val (taxiQuery, constructedQueryDataSource) = parameters[0].second.let { it.value as String to it.source as ConstructedQueryDataSource }
        val query = queryBuilder.buildQuery(schema, taxiQuery)

        val (client, awsConfig) = buildClient(service)

        return Mono.fromFuture(executeQuery(query, client))
            .publishOn(Schedulers.boundedElastic())
            .doOnTerminate {
                try {
                    logger.info { "Closing Aws Lambda Client." }
                    client.close()
                } catch (e: Exception) {
                    logger.error(e) { "Error in closing lambda client" }
                }
            }
            .elapsed()
            .flatMapMany { responsePair ->
                val duration = responsePair.t1
                val response = responsePair.t2
                val count = response.count()
                val remoteCall = buildRemoteCall(service, client, awsConfig, operation, query, duration, count)
                val operationResult = OperationResult.fromTypedInstances(constructedQueryDataSource.inputs, remoteCall)
                eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
                val items = when (response) {
                    is GetItemResponse -> listOf(
                        readItem(
                            response,
                            operation.returnType,
                            schema,
                            operationResult.asOperationReferenceDataSource()
                        )
                    )

                    is QueryResponse -> readItems(
                        response.items(),
                        operation.returnType,
                        schema,
                        operationResult.asOperationReferenceDataSource()
                    )

                    is ScanResponse -> readItems(
                        response.items(),
                        operation.returnType,
                        schema,
                        operationResult.asOperationReferenceDataSource()
                    )

                    else -> error("Not implemented - Response type of ${response::class.simpleName}")
                }
                Flux.fromIterable(items)
            }.asFlow().flowOn(dispatcher)


    }

    private fun readItems(
        items: List<Map<String, AttributeValue>>,
        returnType: Type,
        schema: Schema,
        dataSource: DataSource
    ): List<TypedInstance> {
        return items.map {
            convertToTypedInstance(it, returnType, schema, dataSource)
        }
    }

    private fun buildRemoteCall(
        service: Service,
        client: DynamoDbAsyncClient,
        awsConfig: AwsConnectionConfiguration,
        operation: RemoteOperation,
        query: DynamoDbRequest,
        duration: Long,
        count: Int
    ): RemoteCall = RemoteCall(
        service = service.name,
        address = awsConfig.connectionName,
        operation = operation.name,
        responseTypeName = operation.returnType.name,
        requestBody = query.toString(),
        durationMs = duration,
        timestamp = Instant.now(),
        responseMessageType = ResponseMessageType.FULL,
        response = null,

        exchange = SqlExchange(
            sql = query.toString(),
            recordCount = count
        )

    )

    private fun DynamoDbResponse.count(): Int {
        return when (this) {
            is GetItemResponse -> if (this.hasItem()) 1 else 0
            is QueryResponse -> this.count()
            is ScanResponse -> this.count()
            else -> error("Not implemented - record count for response type ${this::class.simpleName}")
        }
    }

    private fun readItem(
        response: GetItemResponse,
        returnType: Type,
        schema: Schema,
        dataSource: DataSource
    ): TypedInstance {
        if (!response.hasItem()) {
            return TypedNull.create(returnType, dataSource)
        }
        return convertToTypedInstance(response.item(), returnType, schema, dataSource)
    }

    private fun convertToTypedInstance(
        item: Map<String, AttributeValue>,
        returnType: Type,
        schema: Schema,
        dataSource: DataSource
    ): TypedInstance {
        val itemValues: Map<String, Any> = item.map { (key, value) ->
            key to when (value.type()) {
                AttributeValue.Type.N -> BigDecimal(value.n())
                AttributeValue.Type.BOOL -> value.bool()
                AttributeValue.Type.S -> value.s()
                else -> error("Parsing not implemented for type ${value.type().name}")
            }
        }.toMap()
        val memberType = returnType.collectionType ?: returnType
        return TypedInstance.from(memberType, itemValues, schema, source = dataSource)
    }

    private fun executeQuery(
        request: DynamoDbRequest,
        client: DynamoDbAsyncClient
    ): CompletableFuture<out DynamoDbResponse> {
        return when (request) {
            is GetItemRequest -> client.getItem(request)
            is QueryRequest -> client.query(request)
            is ScanRequest -> client.scan(request)
            else -> error("DynamoDbRequest type ${request::class.simpleName} is not supported")
        }
    }

    private fun buildClient(service: Service): Pair<DynamoDbAsyncClient, AwsConnectionConfiguration> {
        val config = getAwsConnectionConfig(service)
        val builder = DynamoDbAsyncClient.builder()
            .configureWithExplicitValuesIfProvided(config)
        return try {
            builder.build() to config
        } catch (e: Exception) {
            logger.error(e) { "Failed to build Dynamo client: ${e.message}" }
            throw e
        }
    }

    private fun getAwsConnectionConfig(service: Service): AwsConnectionConfiguration {
        val connectionName =
            service.metadata(DynamoConnectorTaxi.Annotations.DynamoService.NAME).params["connectionName"] as String
        if (!connectionRegistry.hasConnection(connectionName)) {
            error("Connection $connectionName is not defined")
        }
        val awsConnectionConfiguration = connectionRegistry.getConnection(connectionName)
        logger.info { "AWS connection ${awsConnectionConfiguration.connectionName} with region ${awsConnectionConfiguration.region} found in configurations" }
        return awsConnectionConfiguration
    }
}
