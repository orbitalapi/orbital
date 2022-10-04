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

class ValidationTest : BaseJetIntegrationTest() {
   @Test
   fun `validation is performed for the input types of messages`() {
      val (hazelcastInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName? inherits String
         }
      """
      )

      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Person")
      val pipelineSpec = PipelineSpec(
         name = "test-validation",
         input = BatchItemsSourceSpec(
            listOf(
               """{ "firstName" : "Augustine", "lastName" : null }""",
               """{ "firstName" : "Martin", "lastName" : "McMuller" }""",
               """{ "firstName" : "Marty", "lastName" : "Islander" }"""
            ),
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
         print(listSinkTarget.list.size)
         listSinkTarget.list.size == 2
      }

      val outputValue = listSinkTarget.list.map { (it as StringContentProvider).content }
      outputValue.toSet().should.equal(
         setOf(
            """{ "firstName" : "Martin", "lastName" : "McMuller" }""",
            """{ "firstName" : "Marty", "lastName" : "Islander" }"""
         )
      )
   }
}

