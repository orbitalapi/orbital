package io.vyne.pipelines.jet

import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.schemas.fqn
import org.awaitility.Awaitility.await
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit


class PipelineMetricsTest : BaseJetIntegrationTest() {

   @Test
   @Ignore("Fails on Gitlab")
   fun metricsAreRecordedCorrectly() {
      val (hazelcastInstance, applicationContext, vyneProvider, _, meterRegistry) = jetWithSpringAndVyne(
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

      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec = PipelineSpec(
         "test-pipeline",
         input = FixedItemsSourceSpec(
            items = queueOf(
               """{ "firstName": "jimmy", "lastName": "smith" }""",
               """{ "firstName": "jimmy", "lastName": null }"""
            ),
            typeName = "Person".fqn()
         ),
         outputs = listOf(outputSpec)
      )

      startPipeline(hazelcastInstance, vyneProvider, pipelineSpec)

      val (listSinkTarget2, outputSpec2) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec2 = PipelineSpec(
         "second-pipeline",
         input = FixedItemsSourceSpec(
            items = queueOf(
               """{ "firstName": "jimmy", "lastName": "smith" }""",
               """{ "firstName": "jimmy", "lastName": null }"""
            ),
            typeName = "Person".fqn()
         ),
         outputs = listOf(outputSpec2)
      )

      startPipeline(hazelcastInstance, vyneProvider, pipelineSpec2)


      await().atMost(30, TimeUnit.SECONDS).until {
         val isFirstPipelineCounterCorrect =
            meterRegistry.find("vyne.pipelines.processed").tag("pipeline", "test-pipeline").counter()?.count() == 1.0
               && meterRegistry.find("vyne.pipelines.validationFailed").tag("pipeline", "test-pipeline").counter()
               ?.count() == 1.0

         val isSecondPipelineCounterCorrect =
            meterRegistry.find("vyne.pipelines.processed").tag("pipeline", "second-pipeline").counter()?.count() == 1.0
               && meterRegistry.find("vyne.pipelines.validationFailed").tag("pipeline", "second-pipeline").counter()
               ?.count() == 1.0

         listSinkTarget.list.size == 1 && isFirstPipelineCounterCorrect && listSinkTarget2.list.size == 1 && isSecondPipelineCounterCorrect
      }
   }
}
