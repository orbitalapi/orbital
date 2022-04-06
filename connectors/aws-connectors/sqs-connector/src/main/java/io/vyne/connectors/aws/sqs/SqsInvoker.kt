package io.vyne.connectors.aws.sqs

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import kotlinx.coroutines.flow.Flow

class SqsInvoker(private val schemaProvider: SchemaProvider, private val sqsStreamManager: SqsStreamManager): OperationInvoker {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      val hasParamMatch = operation.parameters.isEmpty()
      return service.hasMetadata(SqsConnectorTaxi.Annotations.SqsService.NAME) &&
         operation.hasMetadata(SqsConnectorTaxi.Annotations.SqsOperation.NAME) &&
         hasParamMatch &&
         streamReturnType(operation) != null
   }

   override suspend fun invoke(service: Service,
                               operation: RemoteOperation,
                               parameters: List<Pair<Parameter, TypedInstance>>, eventDispatcher: QueryContextEventDispatcher, queryId: String?): Flow<TypedInstance> {
      val connectionName = service.metadata(SqsConnectorTaxi.Annotations.SqsService.NAME )
         .params[SqsConnectorTaxi.Annotations.SqsService.ConnectionNameParam] as String
      val sqsOperation = operation.metadata(SqsConnectorTaxi.Annotations.SqsOperation.NAME)
         .let { SqsConnectorTaxi.Annotations.SqsOperation.from(it) }

      return sqsStreamManager.getStream(SnsConsumerRequest(connectionName, sqsOperation.queue, operation.returnType.qualifiedName))
   }

   private fun streamReturnType(operation: RemoteOperation): Type? {
      val retType = schemaProvider.schema().type(operation.returnType.qualifiedName)
      return if (retType.name.name == "Stream") {
         retType.typeParameters[0]
      } else null
   }
}
