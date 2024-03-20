package com.orbitalhq.connectors.nosql.mongodb

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
class MongoDbInvoker(connectionFactory: MongoConnectionFactory,
                     schemaProvider: SchemaProvider): OperationInvoker {
   private val readOnlyInvoker = MongoReadOnlyQueryInvoker(connectionFactory, schemaProvider)
   private val upsertInvoker = MongoMutatingQueryInvoker(connectionFactory, schemaProvider)
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
         operation.operationType == OperationScope.READ_ONLY -> readOnlyInvoker.invoke(service, operation, parameters, eventDispatcher, queryId, queryOptions)

        operation.operationType == OperationScope.MUTATION && operation.hasMetadata("UpsertOperation") -> upsertInvoker.invoke(
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
}
