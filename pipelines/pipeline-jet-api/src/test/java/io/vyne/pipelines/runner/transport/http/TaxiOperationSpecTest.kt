package io.vyne.pipelines.runner.transport.http

import io.vyne.pipelines.jet.api.transport.http.PollingTaxiOperationInputSpec
import io.vyne.pipelines.jet.api.transport.http.TaxiOperationOutputSpec
import io.vyne.pipelines.runner.transport.PipelineTestUtils
import io.vyne.schemas.OperationNames
import org.junit.Test

class TaxiOperationSpecTest {
   @Test
   fun `can read and write output spec from json`() {
      PipelineTestUtils.compareSerializedSpecAndStoreResult(
         output = TaxiOperationOutputSpec(
            OperationNames.name("com.foo.bar.MyService", "listAllUsers")
         )
      )
   }

   @Test
   fun `can read and write polling input spec from json`() {
      PipelineTestUtils.compareSerializedSpecAndStoreResult(
         input = PollingTaxiOperationInputSpec(
            operationName = OperationNames.name("com.foo.bar.MyService", "listAllUsers"),
            pollSchedule = "* * * * * * *",
            parameterMap = mapOf("searchStartDate" to "\$LAST_POLL_TIME")
         )
      )
   }
}
