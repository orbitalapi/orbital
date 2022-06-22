package io.vyne.pipelines.jet.api.transport.aws.sqss3

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.documentation.Maturity
import io.vyne.pipelines.jet.api.documentation.PipelineDocs
import io.vyne.pipelines.jet.api.documentation.PipelineDocumentationSample
import io.vyne.pipelines.jet.api.documentation.PipelineParam
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.http.CronExpression
import io.vyne.pipelines.jet.api.transport.http.CronExpressions

object AwsSqsS3Transport {
   const val TYPE: PipelineTransportType = "awsSnsS3"
   val INPUT = AwsSqsS3TransportInputSpec.specId
   val OUTPUT = AwsSqsS3TransportOutputSpec.specId
}

@PipelineDocs(
   name = "AWS S3 via Sqs",
   docs = """A source that consumes a stream of S3 events via a preconfigured Sqs queue""",
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
   @PipelineParam("A cron expression that defines how frequently to check for new messages.  Defaults to every second")
   val pollSchedule: CronExpression = CronExpressions.EVERY_SECOND,
) : PipelineTransportSpec {
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

   override val description: String = "AWS SNS S3"
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
