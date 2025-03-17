package com.orbitalhq.connectors.aws.dynamodb

import arrow.core.Either
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import lang.taxi.services.OperationScope


class DynamoDbInvoker(
    connectionRegistry: AwsConnectionRegistry,
    schemaProvider: SchemaProvider
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
        queryId: String,
        queryOptions: QueryOptions
    ): Flow<Either<StreamErrorMessage, TypedInstance>> {
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
