package io.vyne.pipelines.runner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.json.parseKeyValuePair
import io.vyne.pipelines.*
import io.vyne.pipelines.runner.events.CollectingEventSink
import io.vyne.pipelines.runner.events.ObserverProvider
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.pipelines.runner.transport.direct.*
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.spring.SimpleVyneProvider
import io.vyne.testVyne
import org.junit.Assert.*
import org.junit.Test
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import java.time.Instant

class PipelineTest {

   @Test
   fun pipelineE2eTest() {
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
      source.send("""{
         | "userId" : "jimmy"
         | }
      """.trimMargin())

      val output = pipelineInstance.output as DirectOutput
      output.messages.should.have.size(1)
      val message = output.messages.first()
      require(message is TypedObject)
      message.type.fullyQualifiedName.should.equal("UserEvent")
      message["id"].value.should.equal("jimmy")
      message["name"].value.should.equal("Jimmy Pitt")
   }
}


class TestSource(val type: Type, val schema: Schema) {
   private val emitter = EmitterProcessor.create<PipelineInputMessage>()

   val flux: Flux<PipelineInputMessage> = emitter
   fun send(message: String) {
      val map = jacksonObjectMapper().readValue<Map<String, Any>>(message)
      val typedInstance = TypedInstance.from(type, map, schema)
      emitter.sink().next(
         PipelineInputMessage(
            messageProvider = { _, _ -> typedInstance },
            messageTimestamp = Instant.now()
         )
      )
   }
}
