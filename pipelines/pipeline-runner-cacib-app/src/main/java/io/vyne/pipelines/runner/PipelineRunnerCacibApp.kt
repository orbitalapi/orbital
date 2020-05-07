package io.vyne.pipelines.runner

import com.cacib.cemaforr.common.record.MatrixRecord
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.*
import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import io.vyne.pipelines.runner.transport.PipelineInputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import io.vyne.pipelines.runner.transport.PipelineTransportSpecId
import io.vyne.pipelines.runner.transport.cask.CaskTransportOutputSpec
import io.vyne.pipelines.runner.transport.kafka.KafkaTransport
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportInputSpec
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportOutputSpec
import io.vyne.schemas.Schema
import io.vyne.spring.SchemaPublicationMethod
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.utils.log
import org.apache.avro.io.DatumReader
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant


@SpringBootApplication
@EnableDiscoveryClient
// TODO : This annotation is misleading, I think there's a better one for clients to use, but
// @EnableVyneClient didn't work. Need to investigate
@VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISTRIBUTED)
@EnableFeignClients(basePackageClasses = [PipelineEventsApi::class])
class PipelineRunnerCacibApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(PipelineRunnerCacibApp::class.java)
         app.run(*args)
      }

      @Bean
      fun pipelineModule() = PipelineJacksonModule(
         listOf(
            KafkaTransportInputSpec.specId,
            CaskTransportOutputSpec.specId,
            KafkaTransportOutputSpec.specId,
            CacibKafkaTransportInputSpec.specId
         )
      )

   }
}

// FIXME inherit later
data class CacibKafkaTransportInputSpec(
   val topic: String,
   override val targetType: VersionedTypeReference,
   val props: Map<String, Any>
) : PipelineTransportSpec {
   companion object {
      val specId = PipelineTransportSpecId("kafka-cacib", PipelineDirection.INPUT, CacibKafkaTransportInputSpec   ::class.java)
   }

   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = "kafka-cacib"
}


// FIXME disables for now
@Component
class CacibKafkaInputBuilder(val objectMapper: ObjectMapper) : PipelineInputTransportBuilder<CacibKafkaTransportInputSpec> {

   override fun canBuild(spec: PipelineTransportSpec) = spec.type == "kafka-cacib" && spec.direction == PipelineDirection.INPUT

   override fun build(spec: CacibKafkaTransportInputSpec): CacibKafkaInput = CacibKafkaInput(spec, objectMapper)

}

// FIXME plain duplicated to test avro serialisation/deserialisation
// Inherid KafkaInput later
class CacibKafkaInput(spec: CacibKafkaTransportInputSpec, objectMapper: ObjectMapper) : PipelineInputTransport {
   protected val defaultProps = mapOf(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.qualifiedName!!,
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.qualifiedName!!
   )
   private val receiverOptions = ReceiverOptions.create<String, ByteArray>(spec.props + defaultProps)
      .commitBatchSize(0) // Don't commit in batches ..  can explore this later
      .commitInterval(Duration.ZERO) // Don't delay commits .. can explore this later
      .subscription(listOf(spec.topic))
      .addAssignListener { partitions -> log().debug("onPartitionsAssigned: $partitions") }
      .addRevokeListener { partitions -> log().debug("onPartitionsRevoked: $partitions") }

   override val feed: Flux<PipelineInputMessage>

   init {
      feed = KafkaReceiver.create(receiverOptions)
         .receive()
         .flatMap { kafkaMessage ->
            val recordId = kafkaMessage.key()
            val offset = kafkaMessage.offset()
            val partition = kafkaMessage.partition()
            val topic = kafkaMessage.topic()
            val headers = kafkaMessage.headers().map { it.key() to it.value().toString(Charset.defaultCharset()) }.toMap()

            val metadata = mapOf(
               "recordId" to recordId,
               "offset" to offset,
               "partition" to partition,
               "topic" to topic,
               "headers" to headers
            )

            val messageProvider = { schema: Schema, logger: PipelineLogger ->
               val targetType = schema.type(spec.targetType)
               logger.debug { "Deserializing record $partition/$offset" }
               val data = kafkaMessage.value()

               logger.debug { "Converting Map to TypeInstance of ${targetType.fullyQualifiedName}" }

               var map = objectMapper.readValue<Map<String, Any>>(deserialize(data))
               TypedInstance.from(targetType, map, schema)
            }
            Mono.create<PipelineInputMessage> { sink ->
               sink.success(PipelineInputMessage(
                  Instant.now(), // TODO : Surely this is in the headers somewhere?
                  metadata,
                  messageProvider
               ))
            }.doOnSuccess {
               kafkaMessage.receiverOffset().acknowledge()
            }
         }
   }

   fun deserialize(bytes: ByteArray): String {
      try {
         var schema = MatrixRecord.getClassSchema()
         val bais = ByteArrayInputStream(bytes)
         val d = DecoderFactory.get().binaryDecoder(bais, null)
         val reader = SpecificDatumReader(MatrixRecord::class.java)
         return  reader.read(null, d).getData()
      } catch (e: IOException) {
         println("Deserialization error:" + e.message)
         throw RuntimeException("ERROR") // FIXME clean
      }
   }

}




