package io.vyne.connectors.kafka

import io.vyne.connectors.kafka.builders.KafkaConnectionBuilder
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.IntegerDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import java.util.Collections


class KafkaInvoker(private val connectionRegistry: KafkaConnectionRegistry, private val schemaProvider: SchemaProvider) : OperationInvoker {

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(KafkaConnectorTaxi.Annotations.KafkaOperation)
   }

   override suspend fun invoke(service: Service, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, eventDispatcher: QueryContextEventDispatcher, queryId: String?): Flow<TypedInstance> {

      val connectionName = service.metadata("io.vyne.kafka.KafkaService").params["connectionName"] as String
      val connectionConfiguration = connectionRegistry.getConnection(connectionName) as DefaultKafkaConnectionConfiguration

      val brokers = connectionConfiguration.connectionParameters.get(KafkaConnectionBuilder.Parameters.BROKERS.param.templateParamName) as String
      val topic = connectionConfiguration.connectionParameters.get(KafkaConnectionBuilder.Parameters.TOPIC.param.templateParamName) as String
      val offset = connectionConfiguration.connectionParameters.get(KafkaConnectionBuilder.Parameters.OFFSET.param.templateParamName) as String

      val consumerProps: MutableMap<String, Any> = HashMap()
      consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
      consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = "vyne-query-server"
      consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
      consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
      consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = offset

      val receiverOptions: ReceiverOptions<Int, String> =
         ReceiverOptions
            .create<Int, String>(consumerProps)
            .subscription(Collections.singleton(topic))

      val inboundFlux: Flux<ReceiverRecord<Int, String>> = KafkaReceiver.create(receiverOptions)
         .receive()


      return inboundFlux
         .map {
            TypedInstance.from(
               operation.returnType.typeParameters.first(),
               it.value()!!,
               schemaProvider.schema(),
               evaluateAccessors = false
            )
      }.asFlow().flowOn(Dispatchers.IO)

   }


}
