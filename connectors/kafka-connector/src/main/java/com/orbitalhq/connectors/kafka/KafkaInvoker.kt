package com.orbitalhq.connectors.kafka

import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.QueryContextSchemaProvider
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lang.taxi.services.OperationScope
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging


class KafkaInvoker(
   private val streamManager: KafkaStreamManager,
   private val streamWriter: KafkaStreamPublisher
) : OperationInvoker {
   private val logger = KotlinLogging.logger {}
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(KafkaConnectorTaxi.Annotations.KafkaService.NAME) && operation.hasMetadata(
         KafkaConnectorTaxi.Annotations.KafkaOperation.NAME
      )
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<TypedInstance> {

      val connectionName = service.metadata("com.orbitalhq.kafka.KafkaService").params["connectionName"] as String
      val kafkaOperation = operation.metadata(KafkaConnectorTaxi.Annotations.KafkaOperation.NAME)
         .let { KafkaConnectorTaxi.Annotations.KafkaOperation.from(it) }

      return if (operation.operationType == OperationScope.MUTATION) {
         return publishToTopic(connectionName, kafkaOperation, service, operation, eventDispatcher, queryId, parameters)
      } else {
         return subscribeToTopic(connectionName, kafkaOperation, service, operation, eventDispatcher, queryId)
      }

   }

   private fun publishToTopic(
      connectionName: String,
      kafkaOperation: KafkaConnectorTaxi.Annotations.KafkaOperation,
      service: Service,
      operation: RemoteOperation,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      parameters: List<Pair<Parameter, TypedInstance>>
   ): Flow<TypedInstance> {
      require(parameters.size == 1) { "Expected a single parameter (the message to publish), but found ${parameters.size}" }

      require(eventDispatcher is QueryContextSchemaProvider) { "EventDispatcher is not a QueryContext, Need a way to access the schema "}
      val schema = eventDispatcher.schema
      // TODO: Get the key from the parameters.
      val key = TypedNull.create(schema.type(PrimitiveType.STRING))

      val messagePayload = parameters.single().second
      return streamWriter.write(
         connectionName, kafkaOperation, service, operation, eventDispatcher, queryId, messagePayload, key, schema
      )
   }

   private fun subscribeToTopic(
      connectionName: String,
      kafkaOperation: KafkaConnectorTaxi.Annotations.KafkaOperation,
      service: Service,
      operation: RemoteOperation,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<TypedInstance> {
      val stream = streamManager.getStream(
         KafkaConsumerRequest(
            connectionName,
            kafkaOperation.topic,
            kafkaOperation.offset,
            service,
            operation
         )
      ).map { instance ->
         val dataSource = instance.source
         require(dataSource is OperationResultDataSourceWrapper) { "Expected OperationResultDataSourceWrapper as the datasource, found ${dataSource::class.simpleName}" }
         eventDispatcher.reportRemoteOperationInvoked(dataSource.operationResult, queryId)

         DataSourceUpdater.update(instance, dataSource.operationResultReferenceSource)
      }

      return stream
   }


}
