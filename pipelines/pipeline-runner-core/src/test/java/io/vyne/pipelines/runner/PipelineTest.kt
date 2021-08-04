package io.vyne.pipelines.runner

import com.jayway.awaitility.Awaitility.await
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.models.json.parseKeyValuePair
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
import io.vyne.pipelines.StringContentProvider
import io.vyne.pipelines.runner.events.ObserverProvider
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.pipelines.runner.transport.direct.DirectInputBuilder
import io.vyne.pipelines.runner.transport.direct.DirectOutput
import io.vyne.pipelines.runner.transport.direct.DirectOutputBuilder
import io.vyne.pipelines.runner.transport.direct.DirectOutputSpec
import io.vyne.pipelines.runner.transport.direct.DirectTransportInputSpec
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

      val source = io.vyne.pipelines.runner.TestSource(vyne.type("PersonLoggedOnEvent"), vyne.schema)
      val pipeline = Pipeline(
         "testPipeline",
         input = PipelineChannel(
            DirectTransportInputSpec(source.flux, VersionedTypeReference("PersonLoggedOnEvent".fqn()))
         ),
         output = PipelineChannel(
            DirectTransportInputSpec(source.flux, VersionedTypeReference("UserEvent".fqn()))
         )
      )

      val pipelineInstance = builder.build(pipeline)

      val json = """{
         | "userId" : "jimmy"
         | }
      """.trimMargin()

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

      val source = io.vyne.pipelines.runner.TestSource(vyne.type("PersonLoggedOnEvent"), vyne.schema)
      val pipeline = Pipeline(
         "testPipeline",
         input = PipelineChannel(
            DirectTransportInputSpec(
               source.flux,
               VersionedTypeReference("PersonLoggedOnEvent".fqn()),

               )
         ),
         output = PipelineChannel(
            DirectOutputSpec(
               messageType = VersionedTypeReference("PersonLoggedOnEvent".fqn()),
            )
         )
      )

      val pipelineInstance = builder.build(pipeline)

      val message = """{
         a,b,f,r
      """.trimMargin()

      source.send(message)

      val input = pipelineInstance.output as DirectOutput
      val output = pipelineInstance.output as DirectOutput
      input.healthMonitor.reportStatus(UP)
      output.healthMonitor.reportStatus(UP)

      await().until { output.messages.should.have.size(1) }

      val outputMessage = output.messages.first()


      outputMessage.should.be.equal(message)
   }

   @Test
   fun pipelineE2eChangingDestination() {
      val (vyne, stub) = PipelineTestUtils.pipelineTestVyne()
      stub.addResponse("getUserNameFromId", vyne.parseKeyValuePair("Username", "Jimmy Pitt"))
      val transportFactory = PipelineTransportFactory(listOf(DirectInputBuilder(), DirectOutputBuilder()))
      val builder = PipelineBuilder(
         transportFactory,
         SimpleVyneProvider(vyne),
         ObserverProvider.local()
      )

      val source = io.vyne.pipelines.runner.TestSource(vyne.type("PersonLoggedOnEvent"), vyne.schema)
      val pipeline = Pipeline(
         "testPipeline",
         input = PipelineChannel(
            DirectTransportInputSpec(
               source.flux,
               VersionedTypeReference("PersonLoggedOnEvent".fqn()),
            )
         ),
         output = PipelineChannel(
            DirectOutputSpec("Default output", VersionedTypeReference("PersonLoggedOnEvent".fqn()))
         )
      )

      val pipelineInstance = builder.build(pipeline)

      val message = """{
         a,b,f,r
      """.trimMargin()

      // Actual testing starts here
      // We want to test that when a message with an overridden output is provided,
      // that the pipeline honours it and routes to the new destination.
      val overriddenOutput =
         transportFactory.buildOutput(DirectOutputSpec("Overridden output", VersionedTypeReference("PersonLoggedOnEvent".fqn())), mock {}) as DirectOutput
      source.send(message, overriddenOutput)

      val input = pipelineInstance.output as DirectOutput
      val defaultOutput = pipelineInstance.output as DirectOutput
      input.healthMonitor.reportStatus(UP)
      defaultOutput.healthMonitor.reportStatus(UP)
      overriddenOutput.healthMonitor.reportStatus(UP)

      // Should've been delivered to the new output
      await().until { overriddenOutput.messages.should.have.size(1) }
      defaultOutput.messages.should.be.empty

      val outputMessage = overriddenOutput.messages.first()
      outputMessage.should.be.equal(message)
   }
}


class TestSource(val type: Type, val schema: Schema) {
   private val emitter = EmitterProcessor.create<PipelineInputMessage>()

   val flux: Flux<PipelineInputMessage> = emitter
   fun send(message: String, overrideOutput: PipelineOutputTransport? = null) {
      emitter.sink().next(
         PipelineInputMessage(
            contentProvider = StringContentProvider(message),
            messageTimestamp = Instant.now(),
            overrideOutput = overrideOutput
         )
      )
   }
}

