package io.vyne.pipelines.jet

import com.hazelcast.jet.core.JobStatus
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.pipelines.PipelineFactory
import io.vyne.pipelines.jet.pipelines.PipelineManager
import io.vyne.pipelines.jet.sink.PipelineSinkProvider
import io.vyne.pipelines.jet.source.PipelineSourceProvider
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.schemas.fqn
import org.awaitility.Awaitility.await
import org.junit.Test
import java.util.concurrent.TimeUnit


class PipelineIntegrationTest : BaseJetIntegrationTest() {

   @Test
   fun pipelineHelloWorldTest() {
      val (jetInstance,applicationContext,vyneProvider) = jetWithSpringAndVyne("""
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """, emptyList())
      val manager = PipelineManager(
         PipelineFactory(
            vyneProvider,
            PipelineSourceProvider.default(),
            PipelineSinkProvider.default(),
         ),
         jetInstance,
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

      await().atMost(10, TimeUnit.SECONDS).until {
         listSinkTarget.list.size == 1
      }
   }
}
