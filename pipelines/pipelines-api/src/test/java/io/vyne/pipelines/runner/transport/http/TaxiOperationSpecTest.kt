package io.vyne.pipelines.runner.transport.http

import io.vyne.pipelines.runner.transport.PipelineTestUtils
import io.vyne.schemas.OperationNames
import org.junit.Test

class TaxiOperationSpecTest {
   @Test
   fun `can read and write from json`() {
      PipelineTestUtils.compareSerializedSpecAndStoreResult(
         output = TaxiOperationOutputSpec(
            OperationNames.name("com.foo.bar.MyService", "listAllUsers")
         )
      )
   }
}
