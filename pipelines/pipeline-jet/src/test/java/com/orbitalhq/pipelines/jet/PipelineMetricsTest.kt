package com.orbitalhq.pipelines.jet

import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.source.fixed.FixedItemsSourceSpec
import com.orbitalhq.schemas.fqn
import org.awaitility.Awaitility.await
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit


class PipelineMetricsTest : BaseJetIntegrationTest() {

   @Test
   @Ignore("Fails on Gitlab")
   fun metricsAreRecordedCorrectly() {
      val testSetup = jetWithSpringAndVyne(
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

      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(testSetup.applicationContext, targetType = "Target")
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

      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)

      val (listSinkTarget2, outputSpec2) = listSinkTargetAndSpec(testSetup.applicationContext, targetType = "Target")
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

      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec2)


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
