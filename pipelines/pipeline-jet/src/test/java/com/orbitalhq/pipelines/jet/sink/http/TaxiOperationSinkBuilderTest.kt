package com.orbitalhq.pipelines.jet.sink.http

import com.winterbe.expekt.should
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.pipelines.jet.BaseJetIntegrationTest
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.http.TaxiOperationOutputSpec
import com.orbitalhq.pipelines.jet.queueOf
import com.orbitalhq.pipelines.jet.source.fixed.FixedItemsSourceSpec
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.fqn
import org.awaitility.Awaitility
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class TaxiOperationSinkBuilderTest : BaseJetIntegrationTest() {
   @Rule
   @JvmField
   val server = MockWebServerRule()

   @Test
   @Ignore("Currently failing, but not used")
   fun `can submit to http service`() {
      val (hazelcastInstance, _, vyneClient) = jetWithSpringAndVyne(
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

      startPipeline(hazelcastInstance, vyneClient, pipelineSpec)
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
