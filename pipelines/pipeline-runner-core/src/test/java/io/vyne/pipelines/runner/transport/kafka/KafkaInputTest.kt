package io.vyne.pipelines.runner.transport.kafka

import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.models.TypedObject
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.DOWN
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
import io.vyne.pipelines.runner.transport.direct.DirectOutput
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner

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
      pipelineInstance.output.reportStatus(UP)

      // Send for messages into kafka
      sendKafkaMessage(""" {"userId":"Marty"} """)
      sendKafkaMessage(""" {"userId":"Paul"} """)
      sendKafkaMessage(""" {"userId":"Andrzej"} """)
      sendKafkaMessage(""" {"userId":"Markus"} """)

      // Wait until we wrote 4 messages in the output
      val output = pipelineInstance.output as DirectOutput
      await().until { output.messages.should.have.size(4) }

      // Check the values in the output
      val outputMessages = output.messages.map { it as TypedObject }.map { it["id"].value as String }
      outputMessages.should.have.all.elements("Marty", "Paul", "Andrzej", "Markus")
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
      pipelineInstance.output.reportStatus(UP)

      // Send for messages into kafka
      sendKafkaMessage(""" {"userId":"Marty"} """)
      sendKafkaMessage(""" {"userId":"Paul"} """)
      await().until { output.messages.should.have.size(2) }

      // Output is now down
      pipelineInstance.output.reportStatus(DOWN)
      await().until { input.isPaused().should.be.`true` }

      // Send 3 other messages
      sendKafkaMessage(""" {"userId":"Eric"} """)
      sendKafkaMessage(""" {"userId":"Andrzej"} """)
      sendKafkaMessage(""" {"userId":"Markus"} """)

      // We shouldn't have more messages incoming
      Thread.sleep(2000) // FIXME use await until
      output.messages.should.have.size(2)

      // Output is back UP
      pipelineInstance.output.reportStatus(UP)
      await().until { output.messages.should.have.size(5) }

      // We should now ingest the 3 new messages. Total of 5
      val outputMessages = output.messages.map { it as TypedObject }.map { it["id"].value as String }
      outputMessages.should.have.all.elements("Marty", "Paul", "Andrzej", "Markus", "Eric")
   }

}
