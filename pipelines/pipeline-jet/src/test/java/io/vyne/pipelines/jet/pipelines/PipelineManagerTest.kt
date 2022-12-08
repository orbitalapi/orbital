package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.core.JobStatus
import com.winterbe.expekt.should
import io.vyne.models.json.parseJson
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.jet.api.transport.query.PollingQueryInputSpec
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.pipelines.jet.source.fixed.ScheduledSourceSpec
import io.vyne.schemas.fqn
import kotlinx.coroutines.flow.flowOf
import org.awaitility.Awaitility
import org.junit.Ignore
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class PipelineManagerTest : BaseJetIntegrationTest() {


   @Test
   fun `can get details of running pipelines`() {
      val (hazelcastInstance, applicationContext, vyneClient) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """, emptyList()
      )
      val manager = pipelineManager(
         hazelcastInstance,
         vyneClient
      )
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec = PipelineSpec(
         "test-pipeline",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName": "smith" }"""),
            typeName = "Person".fqn()
         ),
         outputs = listOf(outputSpec)
      )
      val (_, job) = manager.startPipeline(pipelineSpec)

      assertJobStatusEventually(job, JobStatus.RUNNING, 5)

      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         listSinkTarget.list.size == 1
      }

      val pipelines = manager.getPipelines()
      pipelines.should.have.size(1)
   }

   @Test
   fun `can schedule pipelines`() {
      val (hazelcastInstance, applicationContext, vyneClient) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """, emptyList()
      )
      val manager = pipelineManager(
         hazelcastInstance,
         vyneClient
      )
      val (_, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec = PipelineSpec(
         "test-scheduled-pipeline",
         input = ScheduledSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy" }"""),
            typeName = "Person".fqn(),
            pollSchedule = CronExpressions.EVERY_SECOND
         ),
         outputs = listOf(outputSpec)
      )

      manager.startPipeline(pipelineSpec)

      val pipelines = manager.getPipelines()
      pipelines.should.have.size(1)
      pipelines[0].status.status.should.equal(io.vyne.pipelines.jet.api.JobStatus.SCHEDULED)
   }

   @Test
   fun `can manually trigger a scheduled pipeline`() {
      val testSetup = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         service PeopleService {
            operation getAll(): Person[]
         }
      """
      )

      testSetup.stubService.addResponseFlow("getAll") { _, _ ->
         flowOf(
            parseJson(testSetup.schema, "Person", """{ "firstName": "Jimmy", "lastName": "Fallon" }"""),
            parseJson(testSetup.schema, "Person", """{ "firstName": "Conan", "lastName": "O'Brien" }"""),
            parseJson(testSetup.schema, "Person", """{ "firstName": "Jimmy", "lastName": "Kimmel" }"""),
         )
      }

      val query = """
         find { Person[] }
      """


      val twoHoursLater = Instant.now().plus(2L, ChronoUnit.HOURS).atZone(ZoneId.systemDefault()).hour
      val everyTwoHoursLater = "0 0 $twoHoursLater * * *"
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(testSetup.applicationContext, targetType = "Person")
      val pipelineSpec = PipelineSpec(
         name = "test-query-poll",
         input = PollingQueryInputSpec(
            query,
            everyTwoHoursLater
         ),
         outputs = listOf(outputSpec)
      )

      val pipelineManager = startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec).third

      // manually trigger the execution as its scheduled to run every 5 hours
      pipelineManager.triggerScheduledPipeline(pipelineSpec.id)
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         listSinkTarget.list.size == 3
      }

      val outputValue = listSinkTarget.list.map { (it as TypedInstanceContentProvider).content.toRawObject() }
      outputValue.toSet().should.equal(
         setOf(
            mapOf("firstName" to "Jimmy", "lastName" to "Fallon"),
            mapOf("firstName" to "Conan", "lastName" to "O'Brien"),
            mapOf("firstName" to "Jimmy", "lastName" to "Kimmel"),
         )
      )
   }

   @Test
   @Ignore("this test is failing because of pipeline publication issues - need to investigate")
   fun `can stop running pipeline`() {
      val (hazelcastInstance, applicationContext, vyneClient) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """, emptyList()
      )
      val manager = pipelineManager(
         hazelcastInstance,
         vyneClient
      )
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec = PipelineSpec(
         "test-pipeline",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy" }"""),
            typeName = "Person".fqn()
         ),
         outputs = listOf(outputSpec)
      )
      val (_, job) = manager.startPipeline(pipelineSpec)

      assertJobStatusEventually(job, JobStatus.RUNNING, 5)

      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         listSinkTarget.list.size == 1
      }
      manager.getPipelines().should.have.size(1)

      manager.deletePipeline(pipelineSpec.id)
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         val pipelineSummary = manager.getPipelines().single()
         pipelineSummary.status.status.isTerminal
      }
   }

}
