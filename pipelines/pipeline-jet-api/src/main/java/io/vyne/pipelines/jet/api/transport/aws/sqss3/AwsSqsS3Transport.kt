package io.vyne.pipelines.jet.api.transport.aws.sqss3

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import io.vyne.pipelines.jet.api.transport.http.CronExpression
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import java.net.URI

object AwsSqsS3Transport {
   const val TYPE: PipelineTransportType = "awsSnsS3"
   val INPUT = AwsSqsS3TransportInputSpec.specId
   val OUTPUT = AwsSqsS3TransportOutputSpec.specId
}

open class AwsSqsS3TransportInputSpec(
   val connection: String,
   val targetTypeName: String,
   val mutableProps: MutableMap<String, Any> = hashMapOf(),
   val queueName: String,
   val pollSchedule: CronExpression,
   val endPointOverride: URI? = null
) : PipelineTransportSpec {
   constructor(
      connection: String,
      targetType: VersionedTypeReference,
      props: MutableMap<String, Any> = hashMapOf(),
      queueName: String,
      pollSchedule: CronExpression = CronExpressions.EVERY_SECOND,
      endPointOverride: URI? = null
   ) : this(connection, targetType.toString(), props, queueName, pollSchedule, endPointOverride)

   companion object {
      val specId =
         PipelineTransportSpecId(AwsSqsS3Transport.TYPE, PipelineDirection.INPUT, AwsSqsS3TransportInputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(targetTypeName)
      }

   override val description: String = "AWS SNS S3 props: $props"
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = AwsSqsS3Transport.TYPE

   override val props: Map<String, Any>
      get() = mutableProps.toMap()
}


data class AwsSqsS3TransportOutputSpec(
   val connection: String,
   val bucket: String,
   val objectKey: String,
   val targetTypeName: String,
   override val props: Map<String, Any>,
   val queueName: String,
   val endPointOverride: URI? = null
) : PipelineTransportSpec {
   constructor(
     connection: String,
      bucket: String,
      objectKey: String,
      targetType: VersionedTypeReference,
      props: Map<String, Any>,
      queueName: String,
      endPointOverride: URI? = null
   ) : this(connection, bucket, objectKey, targetType.toString(), props, queueName, endPointOverride)
   companion object {
      val specId =
         PipelineTransportSpecId(AwsSqsS3Transport.TYPE, PipelineDirection.OUTPUT, AwsSqsS3TransportOutputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(this.targetTypeName)
      }
   override val description: String = "AWS S3 props $props"

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = AwsSqsS3Transport.TYPE
}
