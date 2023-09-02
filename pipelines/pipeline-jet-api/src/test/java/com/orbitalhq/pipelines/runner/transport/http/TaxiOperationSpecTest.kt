package com.orbitalhq.pipelines.runner.transport.http

import com.orbitalhq.pipelines.jet.api.transport.http.PollingTaxiOperationInputSpec
import com.orbitalhq.pipelines.jet.api.transport.http.TaxiOperationOutputSpec
import com.orbitalhq.pipelines.runner.transport.PipelineTestUtils
import com.orbitalhq.schemas.OperationNames
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
