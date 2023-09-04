package com.orbitalhq.pipelines.jet.source.http.poll

import com.winterbe.expekt.should
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.pipelines.jet.BaseJetIntegrationTest
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.http.CronExpressions
import com.orbitalhq.pipelines.jet.api.transport.http.PollingTaxiOperationInputSpec
import com.orbitalhq.schemas.OperationNames
import org.awaitility.Awaitility
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

class PollingTaxiOperationSourceBuilderTest : BaseJetIntegrationTest() {

   @Rule
   @JvmField
   val server = MockWebServerRule()

   @Test
   fun `poll from operation and return values`() {
      val (hazelcastInstance, applicationContext, vyneClient) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
         service PersonService {
            @HttpOperation(url = "${server.url("/people")}", method = "GET")
            operation listPeople():Person[]
         }
      """
      )

      server.addJsonResponse(
         """[
         |{ "firstName" : "Jimmy" , "lastName" : "Schmitts" },
         |{ "firstName" : "Jack" , "lastName" : "Schmitts" }
         |]""".trimMargin()
      )

      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = PollingTaxiOperationInputSpec(
            OperationNames.name("PersonService", "listPeople"),
            CronExpressions.EVERY_SECOND
         ),
         outputs = listOf(outputSpec)
      )

      val (_, job) = startPipeline(hazelcastInstance, vyneClient, pipelineSpec)

      // Wait until the next scheduled time is set
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         val metrics = job!!.metrics
         val nextScheduledTime = metrics.get(PollingTaxiOperationSourceBuilder.NEXT_SCHEDULED_TIME_KEY)
         nextScheduledTime.isNotEmpty()
      }

      applicationContext.moveTimeForward(Duration.ofSeconds(2))
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         listSinkTarget.list.isNotEmpty()
      }

      val outputValue = listSinkTarget.firstRawValue()
      outputValue.should.equal(
         listOf(
            mapOf("givenName" to "Jimmy"),
            mapOf("givenName" to "Jack")
         )
      )
   }
}

