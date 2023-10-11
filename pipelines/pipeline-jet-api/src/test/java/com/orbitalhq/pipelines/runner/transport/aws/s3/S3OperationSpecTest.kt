package com.orbitalhq.pipelines.runner.transport.aws.s3

import com.orbitalhq.pipelines.jet.api.transport.aws.s3.AwsS3TransportInputSpec
import com.orbitalhq.pipelines.jet.api.transport.aws.s3.AwsS3TransportOutputSpec
import com.orbitalhq.pipelines.runner.transport.PipelineTestUtils
import org.junit.Test

class S3OperationSpecTest {
   @Test
   fun `can read and write s3 source spec`() {
      val s3SourceSpec = AwsS3TransportInputSpec(
         connectionName = "my-aws-connection",
         bucket = "bucket",
         objectKey = "fileKey",
         targetTypeName = "OrderWindowSummary",
      )
      val s3OutputSpec = AwsS3TransportOutputSpec(
         connectionName = "my-aws-connection",
         bucket = "bucket",
         objectKey = "fileKey",
         targetTypeName = "OrderWindowSummary",
      )

      PipelineTestUtils.compareSerializedSpecAndStoreResult(input = s3SourceSpec, output = s3OutputSpec)
   }
}
