package io.vyne.pipelines.jet.api.transport.aws.s3

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import java.net.URI


object AwsS3Transport {
   const val TYPE: PipelineTransportType = "awsS3"
   val INPUT = AwsS3TransportInputSpec.specId
   val OUTPUT = AwsS3TransportOutputSpec.specId
}

open class AwsS3TransportInputSpec(
   val accessKey: String,
   val secretKey: String,
   val bucket: String,
   val objectKey: String,
   val region: String,
   val targetTypeName: String,
   override val props: Map<String, Any> = emptyMap(),
   val endPointOverride: URI? = null
) : PipelineTransportSpec {
   constructor(
      accessKey: String,
      secretKey: String,
      bucket: String,
      objectKey: String,
      region: String,
      targetType: VersionedTypeReference,
      props: Map<String, Any>,
      endPointOverride: URI? = null
   ) : this(accessKey, secretKey, bucket, objectKey, region, targetType.toString(), props, endPointOverride)

   companion object {
      val specId =
         PipelineTransportSpecId(AwsS3Transport.TYPE, PipelineDirection.INPUT, AwsS3TransportInputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(targetTypeName)
      }

   override val description: String = "AWS S3 props: $props"
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = AwsS3Transport.TYPE
}


data class AwsS3TransportOutputSpec(
   val accessKey: String,
   val secretKey: String,
   val bucket: String,
   val objectKey: String,
   val region: String,
   val targetTypeName: String,
   override val props: Map<String, Any>
) : PipelineTransportSpec {
   constructor(
      accessKey: String,
      secretKey: String,
      bucket: String,
      objectKey: String,
      region: String,
      targetType: VersionedTypeReference,
      props: Map<String, Any>
   ) : this(accessKey, secretKey, bucket, objectKey, region, targetType.toString(), props)
   companion object {
      val specId =
         PipelineTransportSpecId(AwsS3Transport.TYPE, PipelineDirection.OUTPUT, AwsS3TransportOutputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(this.targetTypeName)
      }
   override val description: String = "AWS S3 props $props"

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = AwsS3Transport.TYPE
}
