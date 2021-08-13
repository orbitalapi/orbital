package io.vyne.pipelines.runner.transport.http

import io.vyne.VersionedTypeReference
import io.vyne.annotations.http.HttpOperations
import io.vyne.pipelines.runner.transport.PipelineTestUtils
import io.vyne.schemas.fqn
import org.junit.Test

class HttpListenerTransportSpecTest {

   @Test
   fun `can read and write from json`() {
      PipelineTestUtils.compareSerializedSpecAndStoreResult(
         input = HttpListenerTransportSpec(
            "/foo/bar", HttpOperations.HttpMethod.POST, VersionedTypeReference("com.vyne.MyTestPayload".fqn())
         )
      )
   }
}
