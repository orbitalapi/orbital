package io.vyne.connectors.aws.dynamodb

import io.vyne.connectors.aws.configureWithExplicitValuesIfProvided
import io.vyne.connectors.config.aws.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.query.RemoteCall
import io.vyne.query.ResponseMessageType
import io.vyne.query.SqlExchange
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.schemas.fqn
import mu.KotlinLogging
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.time.Instant
import java.util.concurrent.CompletableFuture

abstract class BaseDynamoInvoker(
    private val connectionRegistry: AwsConnectionRegistry,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    protected fun buildClient(
        service: Service,
        operation: RemoteOperation
    ): Pair<DynamoDbAsyncClient, AwsConnectionConfiguration> {
        val config = getAwsConnectionConfig(service, operation)
        val builder = DynamoDbAsyncClient.builder()
            .configureWithExplicitValuesIfProvided(config)
        return try {
            builder.build() to config
        } catch (e: Exception) {
            logger.error(e) { "Failed to build Dynamo client: ${e.message}" }
            throw e
        }
    }

    private fun getAwsConnectionConfig(service: Service, operation: RemoteOperation): AwsConnectionConfiguration {
        // Look up the connection from the @Table annotation on the return type
        val returnType = operation.returnType.collectionType ?: operation.returnType

        val connectionName =
            returnType.getMetadata(DynamoConnectorTaxi.Annotations.Table.NAME.fqn()).params["connectionName"] as String
        if (!connectionRegistry.hasConnection(connectionName)) {
            error("Connection $connectionName is not defined")
        }
        val awsConnectionConfiguration = connectionRegistry.getConnection(connectionName)
        logger.info { "AWS connection ${awsConnectionConfiguration.connectionName} with region ${awsConnectionConfiguration.region} found in configurations" }
        return awsConnectionConfiguration
    }

    protected fun executeRequest(
        request: DynamoDbRequest,
        client: DynamoDbAsyncClient
    ): CompletableFuture<out DynamoDbResponse> {
        return when (request) {
            is GetItemRequest -> client.getItem(request)
            is QueryRequest -> client.query(request)
            is ScanRequest -> client.scan(request)
            is PutItemRequest -> client.putItem(request)
            else -> error("DynamoDbRequest type ${request::class.simpleName} is not supported")
        }
    }

    protected fun buildRemoteCall(
        service: Service,
        awsConfig: AwsConnectionConfiguration,
        operation: RemoteOperation,
        query: DynamoDbRequest,
        duration: Long,
        count: Int,
        resultCode: Int = -1,
        errorMessage: String? = null,
    ): RemoteCall {
        return RemoteCall(
            service = service.name,
            address = awsConfig.connectionName,
            operation = operation.name,
            responseTypeName = operation.returnType.name,
            requestBody = query.toString(),
            durationMs = duration,
            timestamp = Instant.now(),
            responseMessageType = ResponseMessageType.FULL,
            response = errorMessage,
            isFailed = errorMessage != null,
            resultCode = resultCode,
            exchange = SqlExchange(
                sql = query.toString(),
                recordCount = count
            )

        )
    }
}
