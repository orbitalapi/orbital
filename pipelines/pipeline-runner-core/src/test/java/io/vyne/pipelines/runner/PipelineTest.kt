package io.vyne.pipelines.runner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.awaitility.Awaitility
import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.json.parseKeyValuePair
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
import io.vyne.pipelines.runner.events.ObserverProvider
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.pipelines.runner.transport.direct.*
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.spring.SimpleVyneProvider
import org.junit.Test
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import java.time.Instant

class PipelineTest {

   @Test
   fun pipelineE2eWithTransformation() {
      val (vyne, stub) = PipelineTestUtils.pipelineTestVyne()
      stub.addResponse("getUserNameFromId", vyne.parseKeyValuePair("Username", "Jimmy Pitt"))
      val builder = PipelineBuilder(
         PipelineTransportFactory(listOf(DirectInputBuilder(), DirectOutputBuilder())),
         SimpleVyneProvider(vyne),
         ObserverProvider.local()
      )

      val source = TestSource(vyne.type("PersonLoggedOnEvent"), vyne.schema)
      val pipeline = Pipeline(
         "testPipeline",
         input = PipelineChannel(
            VersionedTypeReference("PersonLoggedOnEvent".fqn()),
            DirectTransportInputSpec(
               source.flux
            )
         ),
         output = PipelineChannel(
            VersionedTypeReference("UserEvent".fqn()),
            DirectOutputSpec
         )
      )

      val pipelineInstance = builder.build(pipeline)

      var json = """{
         | "userId" : "jimmy"
         | }
      """.trimMargin();

      source.send(json)


      val input = pipelineInstance.output as DirectOutput
      val output = pipelineInstance.output as DirectOutput
      input.healthMonitor.reportStatus(UP)
      output.healthMonitor.reportStatus(UP)

      await().until { output.messages.should.have.size(1) }

      val message = output.messages.first()


      message.should.be.equal("""{"id":"jimmy","name":"Jimmy Pitt"}""")
   }

   @Test
   fun pipelineE2eWithoutTransformation() {
      val (vyne, stub) = PipelineTestUtils.pipelineTestVyne()
      stub.addResponse("getUserNameFromId", vyne.parseKeyValuePair("Username", "Jimmy Pitt"))
      val builder = PipelineBuilder(
         PipelineTransportFactory(listOf(DirectInputBuilder(), DirectOutputBuilder())),
         SimpleVyneProvider(vyne),
         ObserverProvider.local()
      )

      val source = TestSource(vyne.type("PersonLoggedOnEvent"), vyne.schema)
      val pipeline = Pipeline(
         "testPipeline",
         input = PipelineChannel(
            VersionedTypeReference("PersonLoggedOnEvent".fqn()),
            DirectTransportInputSpec(
               source.flux
            )
         ),
         output = PipelineChannel(
            VersionedTypeReference("PersonLoggedOnEvent".fqn()),
            DirectOutputSpec
         )
      )

      val pipelineInstance = builder.build(pipeline)

      var message = """{
         a,b,f,r
      """.trimMargin();

      source.send(message)


      val input = pipelineInstance.output as DirectOutput
      val output = pipelineInstance.output as DirectOutput
      input.healthMonitor.reportStatus(UP)
      output.healthMonitor.reportStatus(UP)

      await().until { output.messages.should.have.size(1) }

      val outputMessage = output.messages.first()


      outputMessage.should.be.equal(message)
   }
}


class TestSource(val type: Type, val schema: Schema) {
   private val emitter = EmitterProcessor.create<PipelineInputMessage>()

   val flux: Flux<PipelineInputMessage> = emitter
   fun send(message: String) {
      emitter.sink().next(
         PipelineInputMessage(
            messageProvider = { _ -> message },
            messageTimestamp = Instant.now()
         )
      )
   }
}
