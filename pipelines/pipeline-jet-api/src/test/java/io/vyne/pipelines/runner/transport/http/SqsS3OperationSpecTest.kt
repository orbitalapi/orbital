package io.vyne.pipelines.runner.transport.http

import io.vyne.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.runner.transport.PipelineTestUtils
import org.junit.Test

class SqsS3OperationSpecTest {
   @Test
   fun `can read and write sqs s3 source spec`() {
      PipelineTestUtils.compareSerializedSpecAndStoreResult( AwsSqsS3TransportInputSpec(
        "aws-connection-name",
         "foo",
         hashMapOf(),
         queueName = "sqsQueueUrl",
         pollSchedule = CronExpressions.EVERY_SECOND)
      )
   }
}
