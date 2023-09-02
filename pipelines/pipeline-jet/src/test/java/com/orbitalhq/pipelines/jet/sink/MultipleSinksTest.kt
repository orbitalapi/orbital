package com.orbitalhq.pipelines.jet.sink.http

import com.orbitalhq.models.TypedObject
import com.orbitalhq.pipelines.jet.BaseJetIntegrationTest
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.TypedInstanceContentProvider
import com.orbitalhq.pipelines.jet.queueOf
import com.orbitalhq.pipelines.jet.source.fixed.FixedItemsSourceSpec
import com.orbitalhq.schemas.fqn
import org.awaitility.Awaitility
import org.junit.Test
import java.util.concurrent.TimeUnit

class MultipleSinksTest : BaseJetIntegrationTest() {
   @Test
   fun `can have multiple sinks with different target types`() {
      val (hazelcastInstance, applicationContext, vyneClient) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target1 {
            givenName : FirstName
         }
         model Target2 {
            surname : LastName
         }
      """,
      )

      val (listSinkTarget1, outputSpec1) = listSinkTargetAndSpec(
         applicationContext,
         targetType = "Target1",
         name = "listSink1"
      )
      val (listSinkTarget2, outputSpec2) = listSinkTargetAndSpec(
         applicationContext,
         targetType = "Target2",
         name = "listSink2"
      )
      val pipelineSpec = PipelineSpec(
         "test-multiple-outputs",
         input = FixedItemsSourceSpec(
            items = queueOf(
               """{ "firstName" : "jimmy", "lastName" : "Schmitt" }""",
               """{ "firstName" : "jimmy2", "lastName" : "Schmitt2" }"""
            ),
            typeName = "Person".fqn()
         ),
         outputs = listOf(outputSpec1, outputSpec2)
      )

      startPipeline(hazelcastInstance, vyneClient, pipelineSpec)
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         val areSink1ItemsOfCorrectType =
            listSinkTarget1.list.all { ((it as TypedInstanceContentProvider).content as TypedObject).typeName == "Target1" }
         val sink1Valid = listSinkTarget1.size == 2 && areSink1ItemsOfCorrectType

         val areSink2ItemsOfCorrectType =
            listSinkTarget2.list.all { ((it as TypedInstanceContentProvider).content as TypedObject).typeName == "Target2" }
         val sink2Valid = listSinkTarget2.size == 2 && areSink2ItemsOfCorrectType
         sink1Valid && sink2Valid
      }
   }
}
