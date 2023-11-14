package com.orbitalhq.connectors.aws.dynamodb

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
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
   companion object {
      init {
          DynamoConnectorTaxi.registerConnectorUsage()
      }
   }
    override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
        return service.hasMetadata(DynamoConnectorTaxi.Annotations.DynamoService.NAME)
    }

    private val queryInvoker = DynamoDbQueryInvoker(connectionRegistry, schemaProvider)
    private val upsertInvoker = DynamoDbUpsertInvoker(connectionRegistry, schemaProvider)
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

            operation.operationType == OperationScope.MUTATION && operation.hasMetadata("UpsertOperation") -> upsertInvoker.invoke(
                service,
                operation,
                parameters,
                eventDispatcher,
                queryId
            )

            else -> error("Unhandled Dynamo Operation type: ${operation.qualifiedName.parameterizedName}")
        }
    }
}
