package io.vyne.connectors.aws.dynamodb

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import lang.taxi.services.OperationScope

class DynamoDbInvoker(
    private val connectionRegistry: AwsConnectionRegistry,
    private val schemaProvider: SchemaProvider,
    private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : OperationInvoker {
    override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
        return service.hasMetadata(DynamoConnectorTaxi.Annotations.DynamoService.NAME)
    }

    private val queryInvoker = DynamoDbQueryInvoker(connectionRegistry, schemaProvider)

    override suspend fun invoke(
        service: Service,
        operation: RemoteOperation,
        parameters: List<Pair<Parameter, TypedInstance>>,
        eventDispatcher: QueryContextEventDispatcher,
        queryId: String
    ): Flow<TypedInstance> {
        return when {
            operation.operationType == OperationScope.READ_ONLY -> queryInvoker.invoke(
                service,
                operation,
                parameters,
                eventDispatcher,
                queryId
            )

//            operation.hasMetadata(JdbcConnectorTaxi.Annotations.UpsertOperationAnnotationName) -> upsertInvoker.invoke(
//                service,
//                operation,
//                parameters,
//                eventDispatcher,
//                queryId
//            )

            else -> error("Unhandled Dynamo Operation type: ${operation.qualifiedName.parameterizedName}")
        }
    }
}
