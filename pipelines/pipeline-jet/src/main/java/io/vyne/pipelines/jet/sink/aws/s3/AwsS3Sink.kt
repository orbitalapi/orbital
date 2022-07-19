package io.vyne.pipelines.jet.sink.aws.s3

import com.hazelcast.jet.pipeline.Sink
import io.vyne.connectors.aws.core.accessKey
import io.vyne.connectors.aws.core.endPointOverride
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.aws.core.secretKey
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.format.FormatDetector
import io.vyne.pipelines.jet.api.transport.ConsoleLogger
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportOutputSpec
import io.vyne.pipelines.jet.connectionOrError
import io.vyne.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import mu.KotlinLogging
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI
import java.nio.charset.StandardCharsets

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

      return S3Sinks.s3<MessageContentProvider>(
         pipelineSpec.output.bucket,
         pipelineSpec.output.objectKey,
         pipelineSpec.name,
         StandardCharsets.UTF_8,
         {
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

            s3Builder.build()
         },
         { (message, schema) ->
            val targetType = schema.type(pipelineSpec.output.targetType)
            val typedInstance = message.readAsTypedInstance(ConsoleLogger, targetType, schema)
            val (metadata, _) = FormatDetector(listOf(CsvFormatSpec)).getFormatType(typedInstance.type)!!
            (CsvFormatSpec.serializer.write(typedInstance, metadata) as String).replace("\r\n", "\n")
         }) as Sink<MessageContentProvider>
   }
}
