package com.orbitalhq.pipelines.jet.api.transport.aws.sqss3

import com.orbitalhq.VersionedTypeReference
import com.orbitalhq.pipelines.jet.api.documentation.Maturity
import com.orbitalhq.pipelines.jet.api.documentation.PipelineDocs
import com.orbitalhq.pipelines.jet.api.documentation.PipelineDocumentationSample
import com.orbitalhq.pipelines.jet.api.documentation.PipelineParam
import com.orbitalhq.pipelines.jet.api.transport.CronExpression
import com.orbitalhq.pipelines.jet.api.transport.PipelineDirection
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpecId
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportType
import com.orbitalhq.pipelines.jet.api.transport.ScheduledPipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.aws.s3.AwsS3TransportInputSpec
import com.orbitalhq.pipelines.jet.api.transport.http.CronExpressions

object AwsSqsS3Transport {
   const val TYPE: PipelineTransportType = "awsSqsS3"
   val INPUT = AwsSqsS3TransportInputSpec.specId
   val OUTPUT = AwsSqsS3TransportOutputSpec.specId
}

@PipelineDocs(
   name = "AWS S3 via SQS",
   docs = """A source that consumes a stream of S3 events via a preconfigured SQS queue""",
   sample = AwsS3TransportInputSpec.Sample::class,
   maturity = Maturity.BETA
)
open class AwsSqsS3TransportInputSpec(
   @PipelineParam("The name of the connection, as registered in Vyne's connection manager")
   val connection: String,
   @PipelineParam("The name of the type that content from the S3 bucket should be consumed as")
   val targetTypeName: String,
   @PipelineParam("The name of the SQS queue")
   val queueName: String,
   @PipelineParam("A cron expression that defines how frequently to check for new messages. Defaults to every second.")
   override val pollSchedule: CronExpression = CronExpressions.EVERY_SECOND,
   @PipelineParam("When set to true, specifically controls the next execution time when the last execution finishes.")
   override val preventConcurrentExecution: Boolean = false
) : ScheduledPipelineTransportSpec {
   constructor(
      connection: String,
      targetType: VersionedTypeReference,
      queueName: String,
      pollSchedule: CronExpression = CronExpressions.EVERY_SECOND
   ) : this(connection, targetType.toString(), queueName, pollSchedule)

   object Sample : PipelineDocumentationSample<AwsSqsS3TransportInputSpec> {
      override val sample = AwsSqsS3TransportInputSpec(
         "my-aws-connection",
         "com.demo.customers.Customer",
         "customer-events",
         "* * * * * *"
      )

   }

   companion object {
      val specId =
         PipelineTransportSpecId(
            AwsSqsS3Transport.TYPE,
            PipelineDirection.INPUT,
            AwsSqsS3TransportInputSpec::class.java
         )
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(targetTypeName)
      }

   override val requiredSchemaTypes: List<String>
      get() = listOf(targetTypeName)

   override val description: String = "AWS SQS S3"
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = AwsSqsS3Transport.TYPE

}


data class AwsSqsS3TransportOutputSpec(
   val connection: String,
   val bucket: String,
   val objectKey: String,
   val targetTypeName: String,
   val queueName: String
) : PipelineTransportSpec {
   constructor(
      connection: String,
      bucket: String,
      objectKey: String,
      targetType: VersionedTypeReference,
      queueName: String
   ) : this(connection, bucket, objectKey, targetType.toString(), queueName)

   companion object {
      val specId =
         PipelineTransportSpecId(
            AwsSqsS3Transport.TYPE,
            PipelineDirection.OUTPUT,
            AwsSqsS3TransportOutputSpec::class.java
         )
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(this.targetTypeName)
      }

   override val requiredSchemaTypes: List<String>
      get() = listOf(targetTypeName)

   override val description: String = "AWS S3"

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = AwsSqsS3Transport.TYPE
}
