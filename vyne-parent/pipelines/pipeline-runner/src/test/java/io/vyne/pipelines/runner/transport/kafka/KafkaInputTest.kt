package io.vyne.pipelines.runner.transport.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.awaitility.Awaitility
import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.models.json.parseKeyValuePair
import io.vyne.pipelines.ConsoleLogger
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.pipelines.runner.PipelineBuilder
import io.vyne.pipelines.runner.PipelineTestUtils
import io.vyne.pipelines.runner.events.ObserverProvider
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.pipelines.runner.transport.direct.DirectOutput
import io.vyne.pipelines.runner.transport.direct.DirectOutputBuilder
import io.vyne.pipelines.runner.transport.direct.DirectOutputSpec
import io.vyne.schemas.fqn
import io.vyne.spring.SimpleVyneProvider
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.Test
import reactor.kafka.sender.KafkaSender
import java.time.Duration
import java.util.concurrent.TimeUnit

class KafkaInputTest : AbstractKafkaTest() {

   @Test
   fun canReceiveFromKafkaInput() {
      waitForBrokers()
      val (vyne, stub) = PipelineTestUtils.pipelineTestVyne()
      stub.addResponse("getUserNameFromId", vyne.parseKeyValuePair("Username", "Jimmy Pitt"))
      val builder = PipelineBuilder(
         PipelineTransportFactory(listOf(KafkaInputBuilder(jacksonObjectMapper()), DirectOutputBuilder())),
         SimpleVyneProvider(vyne),
         ObserverProvider(ConsoleLogger())
      )


      val topicName = testName.methodName
      val pipeline = Pipeline(
         "testPipeline",
         input = PipelineChannel(
            VersionedTypeReference("PersonLoggedOnEvent".fqn()),
            KafkaTransportInputSpec(
               topic = topicName,
               targetType = VersionedTypeReference("PersonLoggedOnEvent".fqn()),
               consumerProps = consumerProps("vyne-pipeline-group")
            )
         ),
         output = PipelineChannel(
            VersionedTypeReference("UserEvent".fqn()),
            DirectOutputSpec
         )
      )

      val pipelineInstance = builder.build(pipeline)

      val sender = KafkaSender.create(senderOptions)
      sender.createOutbound()
         .send { records ->
            records.onNext(ProducerRecord(
               topicName,
               "1",
               PipelineTestUtils.personLoggedOnEvent
            ))
         }

      val output = pipelineInstance.output as DirectOutput
      await().atMost(5, TimeUnit.SECONDS).until {
         output.messages.isNotEmpty()
      }
      output.messages.should.have.size(1)
   }
}
