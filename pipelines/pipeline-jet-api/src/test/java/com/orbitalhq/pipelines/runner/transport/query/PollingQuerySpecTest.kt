package com.orbitalhq.pipelines.runner.transport.query

import com.orbitalhq.pipelines.jet.api.transport.query.CronExpressions
import com.orbitalhq.pipelines.jet.api.transport.query.PollingQueryInputSpec
import com.orbitalhq.pipelines.runner.transport.PipelineTestUtils
import org.junit.Test

class PollingQuerySpecTest {
   @Test
   fun `can read and write s3 source spec`() {
      val pollingQuerySourceSpec = PollingQueryInputSpec(
         query = "find { Person( FirstName == 'Jim' ) }",
         pollSchedule = CronExpressions.EVERY_HOUR
      )

      PipelineTestUtils.compareSerializedSpecAndStoreResult(pollingQuerySourceSpec)
   }
}
