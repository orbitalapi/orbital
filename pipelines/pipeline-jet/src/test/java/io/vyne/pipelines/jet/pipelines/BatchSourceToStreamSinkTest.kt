package io.vyne.pipelines.jet.pipelines

import com.winterbe.expekt.should
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.source.fixed.BatchItemsSourceSpec
import io.vyne.schemas.fqn
import org.awaitility.Awaitility
import org.junit.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

class BatchSourceToStreamSinkTest : BaseJetIntegrationTest() {
   @Test
   fun `combining a batch source and streamed sink works`() {
      val (hazelcastInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
      """
      )

      val (listSinkTarget, outputSpec) = streamSinkTargetAndSpec(applicationContext, targetType = "Person")
      val pipelineSpec = PipelineSpec(
         name = "test-batch-to-stream",
         input = BatchItemsSourceSpec(
            listOf("""{ "firstName" : "Martin", "lastName" : "McPitt" }"""),
            "Person".fqn()
         ),
         outputs = listOf(outputSpec)
      )

      startPipeline(
         hazelcastInstance,
         vyneProvider,
         pipelineSpec,
         validateJobStatusIsRunningEventually = false // Status is already completed when checking it
      )

      applicationContext.moveTimeForward(Duration.ofSeconds(2))
      Awaitility.await().atMost(30, TimeUnit.SECONDS).until {
         listSinkTarget.list.size == 1
      }

      val outputValue = listSinkTarget.list.map { (it as StringContentProvider).content }
      outputValue.toSet().should.equal(
         setOf("""{ "firstName" : "Martin", "lastName" : "McPitt" }""")
      )
   }
}

