package io.vyne.pipelines.runner

import io.vyne.VersionedTypeReference
import io.vyne.models.json.parseKeyValuePair
import io.vyne.pipelines.*
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.pipelines.runner.transport.direct.DirectInputBuilder
import io.vyne.pipelines.runner.transport.direct.DirectOutputBuilder
import io.vyne.pipelines.runner.transport.direct.DirectOutputSpec
import io.vyne.pipelines.runner.transport.direct.DirectTransportInputSpec
import io.vyne.schemas.fqn
import io.vyne.spring.SimpleVyneProvider
import io.vyne.testVyne
import org.junit.Assert.*
import org.junit.Test
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux

class PipelineTest {

   @Test
   fun pipelineE2eTest() {
      val src = """
type PersonLoggedOnEvent {
   userId : UserId as String
}
type alias Username as String

service UserService {
   operation getUserNameFromId(UserId):Username
}

type UserEvent {
   id : UserId
   name : Username
}
""".trimIndent()
      val (vyne, stub) = testVyne(src)
      stub.addResponse("getUserNameFromId", vyne.parseKeyValuePair("Username", "Jimmy Pitt"))
      val builder = PipelineBuilder(PipelineTransportFactory(listOf(DirectInputBuilder(), DirectOutputBuilder())), SimpleVyneProvider(vyne))

      val source = TestSource()
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

      val instance = builder.build(pipeline)
       source.send("""{
         | "userId" : "jimmy"
         | }
      """.trimMargin())

      TODO()

   }
}


class TestSource {
   private val emitter = EmitterProcessor.create<PipelineInputMessage>()

   val flux: Flux<PipelineInputMessage> = emitter
   fun send(message: String) {
      emitter.sink().next(
         PipelineInputMessage(
            message.byteInputStream()
         )
      )
   }
}
