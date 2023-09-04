package com.orbitalhq.pipelines.runner.transport.aws.s3

import com.orbitalhq.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import com.orbitalhq.pipelines.jet.api.transport.http.CronExpressions
import com.orbitalhq.pipelines.runner.transport.PipelineTestUtils
import org.junit.Test

class SqsS3OperationSpecTest {
   @Test
   fun `can read and write sqs s3 source spec`() {
      PipelineTestUtils.compareSerializedSpecAndStoreResult(
         AwsSqsS3TransportInputSpec(
            "aws-connection-name",
            "foo",
            queueName = "sqsQueueUrl",
            pollSchedule = CronExpressions.EVERY_SECOND
         )
      )
   }
}
