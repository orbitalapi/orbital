package io.vyne.pipelines.jet.source.http.listener

import io.vyne.VersionedTypeReference
import io.vyne.annotations.http.HttpOperations
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.http.HttpListenerTransportSpec
import org.junit.Test

class HttpListenerSourceTest : BaseJetIntegrationTest() {

   @Test
   fun `listen on http server and push values`() {
      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
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

      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Target")
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = HttpListenerTransportSpec(
            "/foo/bar",
            HttpOperations.HttpMethod.POST,
            VersionedTypeReference.parse("Person")
         ),
         output = outputSpec
      )
      // TODO Implementing the test seems to have been forgotten...
   }
}
