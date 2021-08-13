package io.vyne.pipelines.runner.transport.cask

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.runner.transport.PipelineTestUtils
import io.vyne.schemas.fqn
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
