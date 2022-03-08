package io.vyne.pipelines.runner.transport.http

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportInputSpec
import io.vyne.pipelines.runner.transport.PipelineTestUtils
import org.junit.Test

class S3OperationSpecTest {
   @Test
   fun `can read and write s3 source spec`() {
      val s3SourceSpec = AwsS3TransportInputSpec(
         accessKey =  "aws access key",
         secretKey = "aws secret key",
         bucket = "bucket",
         objectKey = "fileKey",
         region = "ec2-west",
         VersionedTypeReference.parse("OrderWindowSummary"),
         emptyMap(),
      )

      PipelineTestUtils.compareSerializedSpecAndStoreResult(s3SourceSpec)

   }
}
