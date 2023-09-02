package com.orbitalhq.pipelines.jet.sink.aws.s3

import com.hazelcast.jet.pipeline.Sink
import com.orbitalhq.connectors.aws.configureWithExplicitValuesIfProvided
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.models.csv.CsvFormatSpec
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.aws.s3.AwsS3TransportOutputSpec
import com.orbitalhq.pipelines.jet.connectionOrError
import com.orbitalhq.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
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
