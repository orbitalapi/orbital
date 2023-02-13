package io.vyne.connectors.kafka

import io.vyne.models.DataSourceUpdater
import io.vyne.models.OperationResultDataSourceWrapper
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mu.KotlinLogging


class KafkaInvoker(private val streamManager: KafkaStreamManager) : OperationInvoker {
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

      val connectionName = service.metadata("io.vyne.kafka.KafkaService").params["connectionName"] as String
      val kafkaOperation = operation.metadata(KafkaConnectorTaxi.Annotations.KafkaOperation.NAME)
         .let { KafkaConnectorTaxi.Annotations.KafkaOperation.from(it) }

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
