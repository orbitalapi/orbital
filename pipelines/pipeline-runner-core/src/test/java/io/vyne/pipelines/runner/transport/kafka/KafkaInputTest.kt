package io.vyne.pipelines.runner.transport.kafka

import com.google.common.io.ByteStreams
import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.DOWN
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.PipelineTransportType
import io.vyne.pipelines.runner.transport.PipelineInputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.pipelines.runner.transport.PipelineTransportSpecId
import io.vyne.pipelines.runner.transport.direct.DirectOutput
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import java.io.OutputStream

@RunWith(SpringRunner::class)
class KafkaInputTest : AbstractKafkaTest() {

   @Test
   fun canReceiveFromKafkaInput() {
      // Pipeline Kafka -> Direct
      val pipeline = buildPipeline(
         inputTransportSpec = kafkaInputSpec(),
         outputTransportSpec = directOutputSpec()
      )
      val pipelineInstance = buildPipelineBuilder().build(pipeline)
      pipelineInstance.output.healthMonitor.reportStatus(UP)

      // Send for messages into kafka
      sendKafkaMessage(""" {"userId":"Marty"} """)
      sendKafkaMessage(""" {"userId":"Paul"} """)
      sendKafkaMessage(""" {"userId":"Andrzej"} """)
      sendKafkaMessage(""" {"userId":"Markus"} """)

      // Wait until we wrote 4 messages in the output
      val output = pipelineInstance.output as DirectOutput
      await().until { output.messages.should.have.size(4) }

      // Check the values in the output
      output.messages.should.have.all.elements(
         """{"id":"Marty","name":"Marty@mail.com"}""",
         """{"id":"Paul","name":"Paul@mail.com"}""",
         """{"id":"Andrzej","name":"Andrzej@mail.com"}""",
         """{"id":"Markus","name":"Markus@mail.com"}"""
      )
   }

   @Test
   fun canReceiveKafkaInputPaused() {

      // Pipeline Kafka -> Direct
      val pipeline = buildPipeline(
         inputTransportSpec = kafkaInputSpec(),
         outputTransportSpec = directOutputSpec()
      )
      val pipelineInstance = buildPipelineBuilder().build(pipeline)

      val input = pipelineInstance.input as KafkaInput
      val output = pipelineInstance.output as DirectOutput
      pipelineInstance.output.healthMonitor.reportStatus(UP)

      // Send for messages into kafka
      sendKafkaMessage(""" {"userId":"Marty"} """)
      sendKafkaMessage(""" {"userId":"Paul"} """)
      await().until { output.messages.should.have.size(2) }

      // Output is now down
      pipelineInstance.output.healthMonitor.reportStatus(DOWN)
      await().until { input.isPaused().should.be.`true` }

      // Send 3 other messages
      sendKafkaMessage(""" {"userId":"Eric"} """)
      sendKafkaMessage(""" {"userId":"Andrzej"} """)
      sendKafkaMessage(""" {"userId":"Markus"} """)

      // We shouldn't have more messages incoming
      output.messages.should.have.size(2)

      // Output is back UP
      pipelineInstance.output.healthMonitor.reportStatus(UP)
      await().until { output.messages.should.have.size(5) }

      // We should now ingest the 3 new messages. Total of 5
      output.messages.should.have.all.elements(
         """{"id":"Marty","name":"Marty@mail.com"}""",
         """{"id":"Paul","name":"Paul@mail.com"}""",
         """{"id":"Eric","name":"Eric@mail.com"}""",
         """{"id":"Andrzej","name":"Andrzej@mail.com"}""",
         """{"id":"Markus","name":"Markus@mail.com"}"""
      )
   }

   @Test
   fun `corruptMessageIgnored`() {
      // Pipeline CustomKafka -> Direct
      val pipeline = buildPipeline(
         inputTransportSpec = customKafkaTransportInputSpec(),
         outputTransportSpec = directOutputSpec()
      )
      val pipelineInstance = buildPipelineBuilder().build(pipeline)
      pipelineInstance.output.healthMonitor.reportStatus(UP)

      // Send for messages into kafka
      sendKafkaMessage(""" {"userId":"Marty"} """)
      sendKafkaMessage(""" {"userId":"Paul"} """)
      // This will throw in BankKafkaInput::getBody
      sendKafkaMessage(""" {"userId":"Serhat"} """)
      sendKafkaMessage(""" {"userId":"Markus"} """)

      // Wait until we wrote 4 messages in the output
      val output = pipelineInstance.output as DirectOutput
      await().until { output.messages.should.have.size(3) }

      // Check the values in the output
      output.messages.should.have.all.elements(
         """{"id":"Marty","name":"Marty@mail.com"}""",
         """{"id":"Paul","name":"Paul@mail.com"}""",
         """{"id":"Markus","name":"Markus@mail.com"}"""
      )

   }

}

class CustomKafkaTransportInputSpec(topic: String, targetType: VersionedTypeReference, props: Map<String, Any>) : KafkaTransportInputSpec(topic, targetType, props) {

   companion object {
      const val TYPE = "Some-Bank-Spec"
      val specId = PipelineTransportSpecId(TYPE, PipelineDirection.INPUT, CustomKafkaTransportInputSpec::class.java)
   }

   override val type: PipelineTransportType get() = TYPE
}


class CustomKafkaInputBuilder(private val kafkaConnectionFactory:KafkaConnectionFactory<String> = DefaultKafkaConnectionFactory())
   : PipelineInputTransportBuilder<CustomKafkaTransportInputSpec> {

   override fun canBuild(spec: PipelineTransportSpec) = spec.type == CustomKafkaTransportInputSpec.TYPE && spec.direction == PipelineDirection.INPUT

   override fun build(spec: CustomKafkaTransportInputSpec, logger: PipelineLogger, transportFactory: PipelineTransportFactory, pipeline: Pipeline) = BankKafkaInput(spec, transportFactory, logger, kafkaConnectionFactory)
}

class BankKafkaInput(
   spec: CustomKafkaTransportInputSpec,
   transportFactory: PipelineTransportFactory,
   logger: PipelineLogger,
   kafkaConnectionFactory:KafkaConnectionFactory<String> = DefaultKafkaConnectionFactory()) :
   AbstractKafkaInput<String,String>(spec, StringDeserializer::class.qualifiedName!!, transportFactory, logger, kafkaConnectionFactory) {
   override val description: String = spec.description
   override fun type(schema: Schema): Type {
      return schema.type(spec.targetType)
   }

   override fun toMessageContent(payload: String, metadata: Map<String, Any>): MessageContentProvider {

      return object : MessageContentProvider {
         override fun readAsTypedInstance(logger: PipelineLogger, inputType: Type, schema: Schema): TypedInstance {
            TODO("Not yet implemented")
         }

         override fun asString(logger: PipelineLogger): String {
            logger.debug { "Deserializing record partition=${metadata["partition"]}/ offset=${metadata["offset"]}" }
            return payload
         }
         override fun writeToStream(logger: PipelineLogger, outputStream: OutputStream) {
            // Step 1. Get the message
            logger.debug { "Deserializing record partition=${metadata["partition"]}/ offset=${metadata["offset"]}" }
            ByteStreams.copy(payload.byteInputStream(), outputStream)
         }
      }
   }
   override fun getBody(message: String): String {
      if (message.contains("Serhat", false)) {
         throw  IllegalStateException("Serhat is not allowed")
      }
      return message
   }

}
