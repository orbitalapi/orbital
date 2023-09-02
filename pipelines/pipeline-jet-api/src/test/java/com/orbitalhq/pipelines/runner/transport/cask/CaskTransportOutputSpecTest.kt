package com.orbitalhq.pipelines.runner.transport.cask

import com.orbitalhq.VersionedTypeReference
import com.orbitalhq.pipelines.jet.api.transport.cask.CaskTransportOutputSpec
import com.orbitalhq.pipelines.runner.transport.PipelineTestUtils
import com.orbitalhq.schemas.fqn
import org.junit.Test

class CaskTransportOutputSpecTest {
   @Test
   fun `can read and write from json`() {
      PipelineTestUtils.compareSerializedSpecAndStoreResult(
         output = CaskTransportOutputSpec(
            targetType = VersionedTypeReference("com.foo.bar.CaskType".fqn())
         )
      )
   }
}
