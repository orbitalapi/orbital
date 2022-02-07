package io.vyne.connectors.kafka

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging



class KafkaInvoker(private val streamManager: KafkaStreamManager) : OperationInvoker {
   private val logger = KotlinLogging.logger {}
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(KafkaConnectorTaxi.Annotations.KafkaService.NAME) && operation.hasMetadata(KafkaConnectorTaxi.Annotations.KafkaOperation.NAME)
   }

   override suspend fun invoke(service: Service, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, eventDispatcher: QueryContextEventDispatcher, queryId: String?): Flow<TypedInstance> {

      val connectionName = service.metadata("io.vyne.kafka.KafkaService").params["connectionName"] as String
      val kafkaOperation = operation.metadata(KafkaConnectorTaxi.Annotations.KafkaOperation.NAME).let { KafkaConnectorTaxi.Annotations.KafkaOperation.from(it) }

      val stream = streamManager.getStream(KafkaConsumerRequest(
         connectionName,
         kafkaOperation.topic,
         kafkaOperation.offset,
         operation.returnType.qualifiedName
      ))

      return stream


//      val brokers = connectionConfiguration.brokers
//      val groupId = connectionConfiguration.groupId
//
//      val topic = kafkaOperation.topic
//      val offset = kafkaOperation.offset.toString().toLowerCase()
//
//      val consumerProps: MutableMap<String, Any> = HashMap()
//      consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
//      consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = groupId
//      consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
//      consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
//      consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = offset
//
//      val receiverOptions: ReceiverOptions<Int, String> =
//         ReceiverOptions
//            .create<Int, String>(consumerProps)
//            .subscription(Collections.singleton(topic))
//
//      val inboundFlux: Flux<ReceiverRecord<Int, String>> = KafkaReceiver.create(receiverOptions)
//         .receive()
//
//
//      return inboundFlux
//         .map {
//            logger.debug { "Received message on topic ${it.topic()} with offset ${it.offset()}" }
//            TypedInstance.from(
//               operation.returnType.typeParameters.first(),
//               it.value()!!,
//               schemaProvider.schema(),
//               evaluateAccessors = false
//            )
//      }.asFlow().flowOn(Dispatchers.IO)

   }


}
