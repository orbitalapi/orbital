package io.vyne.pipelines.jet.source.aws.s3

import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.s3.S3Sources
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportInputSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.PipelineSourceType
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.nio.charset.StandardCharsets

class S3SourceBuilder : PipelineSourceBuilder<AwsS3TransportInputSpec> {

   override val sourceType: PipelineSourceType
      get() = PipelineSourceType.Batch

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is AwsS3TransportInputSpec
   }

   override fun buildBatch(pipelineSpec: PipelineSpec<AwsS3TransportInputSpec, *>): BatchSource<MessageContentProvider>? {
      val bucketName = pipelineSpec.input.bucket
      return S3Sources.s3(listOf(bucketName), pipelineSpec.input.objectKey, StandardCharsets.UTF_8, {
         val builder = S3Client
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(pipelineSpec.input.accessKey, pipelineSpec.input.secretKey)))
            .region(Region.of(pipelineSpec.input.region))

         if (pipelineSpec.input.endPointOverride != null) {
            builder.endpointOverride(pipelineSpec.input.endPointOverride)
         }

          builder.build()
      }) { _, line ->
         StringContentProvider(line)
      }
   }


   override fun getEmittedType(pipelineSpec: PipelineSpec<AwsS3TransportInputSpec, *>, schema: Schema): QualifiedName {
      return pipelineSpec.input.targetType.typeName
   }
}
