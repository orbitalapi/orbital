package com.orbitalhq.pipelines.runner.transport.http

import com.orbitalhq.VersionedTypeReference
import com.orbitalhq.annotations.http.HttpOperations
import com.orbitalhq.pipelines.jet.api.transport.http.HttpListenerTransportSpec
import com.orbitalhq.pipelines.runner.transport.PipelineTestUtils
import com.orbitalhq.schemas.fqn
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
