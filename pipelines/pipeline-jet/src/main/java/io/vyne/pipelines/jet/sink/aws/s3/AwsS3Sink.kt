package io.vyne.pipelines.jet.sink.aws.s3

import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.connectors.aws.core.accessKey
import io.vyne.connectors.aws.core.endPointOverride
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.aws.core.secretKey
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.format.FormatDetector
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportOutputSpec
import io.vyne.pipelines.jet.connectionOrError
import io.vyne.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.spring.VyneProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI
import javax.annotation.Resource

@Component
class AwsS3SinkBuilder(private val connectionRegistry: AwsConnectionRegistry) :
   SingleMessagePipelineSinkBuilder<AwsS3TransportOutputSpec> {
   companion object {
      val logger = KotlinLogging.logger { }
   }

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean = pipelineSpec.output is AwsS3TransportOutputSpec

   override fun getRequiredType(
      pipelineSpec: PipelineSpec<*, AwsS3TransportOutputSpec>,
      schema: Schema
   ): QualifiedName {
      return pipelineSpec.output.targetType.typeName
   }

   override fun build(pipelineSpec: PipelineSpec<*, AwsS3TransportOutputSpec>): Sink<MessageContentProvider> {
      val connection = connectionRegistry.connectionOrError(pipelineSpec.id, pipelineSpec.output.connectionName)

      return SinkBuilder
         .sinkBuilder("aws-s3-sink") { context -> AwsS3SinkContext(context.logger(), pipelineSpec) }
         .receiveFn { context: AwsS3SinkContext, item: MessageContentProvider ->
            val s3Builder = S3Client
               .builder()
               .credentialsProvider(
                  StaticCredentialsProvider.create(
                     AwsBasicCredentials.create(
                        connection.accessKey,
                        connection.secretKey
                     )
                  )
               )
               .region(Region.of(connection.region))

            val endPointOverride = connection.endPointOverride
            if (endPointOverride != null) {
               s3Builder.endpointOverride(URI(endPointOverride))
            }
            val s3 = s3Builder.build()
            val putObjectRequest = PutObjectRequest.builder().bucket(context.pipelineSpec.output.bucket)
               .key(context.pipelineSpec.output.objectKey).build()

            val content = (item as TypedInstanceContentProvider).content
            val (metadata, _) = FormatDetector(listOf(CsvFormatSpec)).getFormatType(content.type)!!
            val generated = (CsvFormatSpec.serializer.write(content, metadata) as String).replace("\r\n", "\n")
            s3.putObject(putObjectRequest, RequestBody.fromString(generated))
         }
         .build()
   }
}

@SpringAware
class AwsS3SinkContext(
   val logger: ILogger,
   val pipelineSpec: PipelineSpec<*, AwsS3TransportOutputSpec>
) {
   val outputSpec: AwsS3TransportOutputSpec = pipelineSpec.output

   @Resource
   lateinit var vyneProvider: VyneProvider
}
