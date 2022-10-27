package io.vyne.pipelines.jet.sink.aws.s3

import com.hazelcast.jet.pipeline.Sink
import io.vyne.connectors.aws.core.configureWithExplicitValuesIfProvided
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.format.FormatDetector
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportOutputSpec
import io.vyne.pipelines.jet.connectionOrError
import io.vyne.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import mu.KotlinLogging
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import java.nio.charset.StandardCharsets

@Component
class AwsS3SinkBuilder(private val connectionRegistry: AwsConnectionRegistry) :
   SingleMessagePipelineSinkBuilder<AwsS3TransportOutputSpec> {
   companion object {
      val logger = KotlinLogging.logger { }
   }

   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean =
      pipelineTransportSpec is AwsS3TransportOutputSpec

   override fun getRequiredType(
      pipelineTransportSpec: AwsS3TransportOutputSpec,
      schema: Schema
   ): QualifiedName {
      return pipelineTransportSpec.targetType.typeName
   }

   override fun build(
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: AwsS3TransportOutputSpec
   ): Sink<MessageContentProvider> {
      val connection = connectionRegistry.connectionOrError(pipelineId, pipelineTransportSpec.connectionName)

      return S3Sinks.s3(
         pipelineTransportSpec.bucket,
         pipelineTransportSpec.objectKey,
         pipelineName,
         StandardCharsets.UTF_8,
         {
            val s3Builder = S3Client
               .builder()
               .configureWithExplicitValuesIfProvided(connection)
            s3Builder.build()
         },
         { (message, schema) ->
            val targetType = schema.type(pipelineTransportSpec.targetType)
            val typedInstance = message.readAsTypedInstance(targetType, schema)
            val (metadata, _) = FormatDetector(listOf(CsvFormatSpec)).getFormatType(typedInstance.type)!!
            (CsvFormatSpec.serializer.write(typedInstance, metadata) as String).replace("\r\n", "\n")
         }) as Sink<MessageContentProvider>
   }
}
