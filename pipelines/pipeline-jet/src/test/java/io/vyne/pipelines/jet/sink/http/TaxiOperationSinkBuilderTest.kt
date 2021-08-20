package io.vyne.pipelines.jet.sink.http

import com.winterbe.expekt.should
import io.vyne.http.MockWebServerRule
import io.vyne.pipelines.PipelineSpec
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.pipelines.runner.transport.http.TaxiOperationOutputSpec
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
      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
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
      """
      )
      val pipelineSpec = PipelineSpec(
         "test-http-sink",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn()
         ),
         output = TaxiOperationOutputSpec(
            OperationNames.name("PersonService", "trackPersonEvent")
         )
      )

      val (pipeline,job) = startPipeline(jetInstance, vyneProvider, pipelineSpec)
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         server.requestCount > 0
      }
      val request = server.takeRequest()
      val body =request.body.readByteString().toString()
         .removeSurrounding("[text=", "]")
      body.should.equal("""{"givenName":"jimmy"}""")
   }
}
