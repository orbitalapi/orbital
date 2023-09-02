package com.orbitalhq.pipelines.jet

import com.hazelcast.jet.core.JobStatus
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.pipelines.PipelineFactory
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import com.orbitalhq.pipelines.jet.source.fixed.FixedItemsSourceSpec
import com.orbitalhq.schemas.fqn
import org.awaitility.Awaitility.await
import org.junit.Test
import java.util.concurrent.TimeUnit


class PipelineIntegrationTest : BaseJetIntegrationTest() {

   @Test
   fun pipelineHelloWorldTest() {
      val (hazelcastInstance, applicationContext, vyneClient) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """
      )
      val manager = PipelineManager(
         PipelineFactory(
            vyneClient,
            pipelineSourceProvider,
            pipelineSinkProvider,
         ),
         hazelcastInstance,
      )

      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec = PipelineSpec(
         "test-pipeline",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName": "jimmy", "lastName": "smith" }"""),
            typeName = "Person".fqn()
         ),
         outputs = listOf(outputSpec)
      )
      val (_, job) = manager.startPipeline(pipelineSpec)

      assertJobStatusEventually(job, JobStatus.RUNNING, 5)

      await().atMost(10, TimeUnit.SECONDS).until {
         listSinkTarget.list.size == 1
      }
   }
}
