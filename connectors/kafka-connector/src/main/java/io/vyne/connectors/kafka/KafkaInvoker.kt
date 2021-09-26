package io.vyne.connectors.kafka

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.types.CompilationUnit
import lang.taxi.types.StreamType
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.IntegerDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import java.util.Collections


class KafkaInvoker(private val schemaProvider: SchemaProvider) : OperationInvoker {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(KafkaConnectorTaxi.Annotations.KafkaOperation)
   }

   override suspend fun invoke(service: Service, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, eventDispatcher: QueryContextEventDispatcher, queryId: String?): Flow<TypedInstance> {

      val topic = service.metadata("io.vyne.kafka.KafkaService").params["topic"] as String
      println("Invoking!! KafkaInvoker -- ${service.metadata} topic = $topic"  )

      val consumerProps: MutableMap<String, Any> = HashMap()
      consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:29092, localhost:39092"
      consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = "vyne-query-server"
      consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = IntegerDeserializer::class.java
      consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java

      val receiverOptions: ReceiverOptions<Int, String> =
         ReceiverOptions
            .create<Int, String>(consumerProps)
            .subscription(Collections.singleton(topic))

      val inboundFlux: Flux<ReceiverRecord<Int, String>> = KafkaReceiver.create(receiverOptions)
         .receive()


      return inboundFlux
         .map {

            println("Return Type = [${operation.returnType.typeParameters[0]}]")
            println("Int on stream = [${it.key()}]")
            println("Value on stream = [${it.value()}]")

            val ti = TypedInstance.from(
               operation.returnType.typeParameters.first(),
               it.value()!!,
               schemaProvider.schema(),
               evaluateAccessors = false
            )
         println(ti)
            ti
      }.asFlow().flowOn(Dispatchers.IO)

   }

}
