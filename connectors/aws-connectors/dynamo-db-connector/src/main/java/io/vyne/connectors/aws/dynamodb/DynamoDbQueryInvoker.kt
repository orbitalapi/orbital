package io.vyne.connectors.aws.dynamodb

import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.query.ConstructedQueryDataSource
import io.vyne.query.QueryContextEventDispatcher
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
import software.amazon.awssdk.services.dynamodb.model.*
import java.math.BigDecimal

class DynamoDbQueryInvoker(
    private val connectionRegistry: AwsConnectionRegistry,
    private val schemaProvider: SchemaProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseDynamoInvoker(connectionRegistry) {
    private val queryBuilder = DynamoDbRequestBuilder()

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
        val (client, awsConfig) = buildClient(service, operation)

        return Mono.fromFuture(executeRequest(query, client))
            .doOnError { e ->
                val errorCode = when (e) {
                    is DynamoDbException -> e.statusCode()
                    else -> 400
                }
                val message = "Call to Dynamo failed: ${e.message ?: e.toString()}"
                val remoteCall = buildRemoteCall(service, awsConfig, operation, query, -1, -1, errorCode, message)
                val operationResult = OperationResult.fromTypedInstances(constructedQueryDataSource.inputs, remoteCall)
                eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
                logger.error(e) { message }
            }
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
                val remoteCall = buildRemoteCall(service, awsConfig, operation, query, duration, count)
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

}
