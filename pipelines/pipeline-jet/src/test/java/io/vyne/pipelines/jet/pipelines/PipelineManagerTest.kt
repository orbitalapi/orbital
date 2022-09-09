package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.core.JobStatus
import com.winterbe.expekt.should
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.pipelines.jet.source.fixed.ScheduledSourceSpec
import io.vyne.schemas.fqn
import org.awaitility.Awaitility
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit

class PipelineManagerTest : BaseJetIntegrationTest() {


   @Test
   fun `can get details of running pipelines`() {
      val (hazelcastInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
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
         vyneProvider
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

      val pipelines = manager.getPipelines()
      pipelines.should.have.size(1)
   }

   @Test
   fun `can schedule pipelines`() {
      val (hazelcastInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
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
         vyneProvider
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
   @Ignore("this test is failing because of pipeline publication issues - need to investigate")
   fun `can stop running pipeline`() {
      val (hazelcastInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
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
         vyneProvider
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
