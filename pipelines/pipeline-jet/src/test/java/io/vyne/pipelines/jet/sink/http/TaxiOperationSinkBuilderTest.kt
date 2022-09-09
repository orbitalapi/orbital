package io.vyne.pipelines.jet.sink.http

import com.winterbe.expekt.should
import io.vyne.http.MockWebServerRule
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.http.TaxiOperationOutputSpec
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.schemas.OperationNames
import io.vyne.schemas.fqn
import org.awaitility.Awaitility
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class TaxiOperationSinkBuilderTest : BaseJetIntegrationTest() {
   @Rule
   @JvmField
   val server = MockWebServerRule()

   @Test
   fun `can submit to http service`() {
      val (hazelcastInstance, _, vyneProvider) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
         service PersonService {
            @HttpOperation(url = "${server.url("/people")}", method = "POST")
            operation trackPersonEvent(@RequestBody target:Target)
         }
      """, emptyList()
      )
      val pipelineSpec = PipelineSpec(
         "test-http-sink",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn()
         ),
         outputs = listOf(
            TaxiOperationOutputSpec(
               OperationNames.name("PersonService", "trackPersonEvent")
            )
         )
      )

      startPipeline(hazelcastInstance, vyneProvider, pipelineSpec)
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         server.requestCount > 0
      }
      val request = server.takeRequest(10L)
      val body = request.body.readByteString().toString()
         .removeSurrounding("[text=", "]")
      body.should.equal("""{"givenName":"jimmy"}""")
      terminateInstance(hazelcastInstance)
   }
}
