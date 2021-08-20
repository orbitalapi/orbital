package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.core.JobStatus
import com.winterbe.expekt.should
import io.vyne.pipelines.PipelineSpec
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.schemas.fqn
import org.awaitility.Awaitility
import org.junit.Test
import java.util.concurrent.TimeUnit

class PipelineManagerTest : BaseJetIntegrationTest() {

   @Test
   fun `can get details of running pipelines`() {
      val (jetInstance,applicationContext,vyneProvider) = jetWithSpringAndVyne("""
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """)
      val manager = pipelineManager(
         jetInstance,
         vyneProvider
      )
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec = PipelineSpec(
         "test-pipeline",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy" }"""),
            typeName = "Person".fqn()
         ),
         output = outputSpec
      )
      val (pipeline, job) = manager.startPipeline(pipelineSpec)

      assertJobStatusEventually(job, JobStatus.RUNNING, 5)

      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         listSinkTarget.list.size == 1
      }

      val pipelines = manager.getPipelines()
      pipelines.should.have.size(1)
   }

   @Test
   fun `can stop running pipeline`() {
      val (jetInstance,applicationContext,vyneProvider) = jetWithSpringAndVyne("""
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """)
      val manager = pipelineManager(
         jetInstance,
         vyneProvider
      )
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec = PipelineSpec(
         "test-pipeline",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy" }"""),
            typeName = "Person".fqn()
         ),
         output = outputSpec
      )
      val (pipeline, job) = manager.startPipeline(pipelineSpec)

      assertJobStatusEventually(job, JobStatus.RUNNING, 5)

      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         listSinkTarget.list.size == 1
      }
      manager.getPipelines().should.have.size(1)

      val status = manager.deletePipeline(pipelineSpec.id)
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         val pipelineSummary = manager.getPipelines().single()
         pipelineSummary.status.status.isTerminal
      }
   }

}
