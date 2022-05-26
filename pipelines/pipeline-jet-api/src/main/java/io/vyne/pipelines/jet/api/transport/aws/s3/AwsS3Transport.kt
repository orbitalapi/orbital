package io.vyne.pipelines.jet.api.transport.aws.s3

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.documentation.Maturity
import io.vyne.pipelines.jet.api.documentation.PipelineDocs
import io.vyne.pipelines.jet.api.documentation.PipelineDocumentationSample
import io.vyne.pipelines.jet.api.documentation.PipelineParam
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


@PipelineDocs(
   name = "AWS S3",
   docs = """A source that consumes a single file/object from an S3 bucket.""",
   sample = AwsS3TransportInputSpec.Sample::class,
   maturity = Maturity.BETA
)
open class AwsS3TransportInputSpec(
   @PipelineParam("The name of the connection, as registered in Vyne's connection manager")
   val connectionName: String,
   @PipelineParam("The bucket name")
   val bucket: String,
   @PipelineParam("The name of the object in the S3 bucket - generally a file name")
   val objectKey: String,
   @PipelineParam("The name of the type that content from the S3 bucket should be consumed as")
   val targetTypeName: String,
   @PipelineParam(
      "Allows consuming from a different S3 endpoint.  Used where customers have their own on-site S3 infrastructure",
      supressFromDocs = true
   )
   val endPointOverride: URI? = null
) : PipelineTransportSpec {


   object Sample : PipelineDocumentationSample<PipelineTransportSpec> {
      override val sample: PipelineTransportSpec = AwsS3TransportInputSpec(
         "my-aws-connection",
         "my-bucket",
         "customers.csv",
         "com.demo.customers.Customer"
      )

   }

   companion object {
      val specId =
         PipelineTransportSpecId(AwsS3Transport.TYPE, PipelineDirection.INPUT, AwsS3TransportInputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(targetTypeName)
      }

   override val description: String = "S3 Input for connection $connectionName and bucket $bucket"
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = AwsS3Transport.TYPE
}


@PipelineDocs(
   name = "AWS S3",
   docs = """An output that produces a single file to an S3 bucket.""",
   sample = AwsS3TransportOutputSpec.Sample::class,
   maturity = Maturity.BETA
)
data class AwsS3TransportOutputSpec(
   @PipelineParam("The name of the connection, as registered in Vyne's connection manager")
   val connectionName: String,
   @PipelineParam("The bucket name")
   val bucket: String,
   @PipelineParam("The name of the object in the S3 bucket - generally a file name. To obtain unique file names you can use the \"{env.now}\" as part of the object key to add a timestamp in the ISO format (YYYY-MM-DDTHH:mm:ss.sssZ). If the object exists already, it will be overwritten.")
   val objectKey: String,
   @PipelineParam("The name of the type based on which to produce the file")
   val targetTypeName: String,
) : PipelineTransportSpec {
   companion object {
      val specId =
         PipelineTransportSpecId(AwsS3Transport.TYPE, PipelineDirection.OUTPUT, AwsS3TransportOutputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(this.targetTypeName)
      }
   override val description: String = "S3 Output for connection $connectionName and bucket $bucket"

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = AwsS3Transport.TYPE

   object Sample : PipelineDocumentationSample<PipelineTransportSpec> {
      override val sample: PipelineTransportSpec = AwsS3TransportOutputSpec(
         "my-aws-connection",
         "my-bucket",
         "customers.csv",
         "com.demo.customers.Customer"
      )

   }
}
