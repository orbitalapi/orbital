package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.aggregate.AggregateOperations
import com.hazelcast.jet.pipeline.GeneralStage
import com.hazelcast.jet.pipeline.Pipeline
import com.hazelcast.jet.pipeline.StreamStage
import com.hazelcast.jet.pipeline.WindowDefinition
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.WindowingPipelineTransportSpec
import io.vyne.pipelines.jet.sink.PipelineSinkBuilder
import io.vyne.pipelines.jet.sink.PipelineSinkProvider
import io.vyne.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import io.vyne.pipelines.jet.sink.WindowingPipelineSinkBuilder
import io.vyne.pipelines.jet.source.PipelineSourceProvider
import io.vyne.pipelines.jet.source.PipelineSourceType
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.spring.VyneProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class PipelineFactory(
   private val vyneProvider: VyneProvider,
   private val sourceProvider: PipelineSourceProvider,
   private val sinkProvider: PipelineSinkProvider
) {
   private val logger = KotlinLogging.logger {}

   fun <I : PipelineTransportSpec, O : PipelineTransportSpec> createJetPipeline(pipelineSpec: PipelineSpec<I, O>): Pipeline {
      val jetPipeline = Pipeline.create()
      val vyne = vyneProvider.createVyne()
      val sourceBuilder = sourceProvider.getPipelineSource(pipelineSpec)
      val schema = vyne.schema
      val inputTypeName = sourceBuilder.getEmittedType(pipelineSpec, schema)
      val inputType = if (inputTypeName != null) schema.type(inputTypeName) else null

      val jetPipelineBuilder = if (sourceBuilder.sourceType == PipelineSourceType.Stream) {
         jetPipeline
            .readFrom(sourceBuilder.build(pipelineSpec, inputType)!!)
            .withIngestionTimestamps()
            .setName("Ingest from ${pipelineSpec.input.description}")
      } else {
         jetPipeline
            .readFrom(sourceBuilder.buildBatch(pipelineSpec, inputType)!!)
            .setName("Ingest from ${pipelineSpec.input.description}")
      }

      pipelineSpec.outputs.forEach { output ->
         buildTransformAndSinkStageForOutput(
            inputTypeName,
            schema,
            pipelineSpec.id,
            pipelineSpec.name,
            output,
            jetPipelineBuilder
         )
      }

      return jetPipeline
   }

   private fun buildTransformAndSinkStageForOutput(
      inputType: QualifiedName?,
      schema: Schema,
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: PipelineTransportSpec,
      jetPipelineBuilder: GeneralStage<MessageContentProvider>
   ) {
      val sinkBuilder = sinkProvider.getPipelineSink(pipelineTransportSpec)
      val outputTypeName = sinkBuilder.getRequiredType(pipelineTransportSpec, schema)
      val jetPipelineWithTransformation = buildTransformStage(inputType, outputTypeName, jetPipelineBuilder)
      buildSink(pipelineId, pipelineName, pipelineTransportSpec, sinkBuilder, jetPipelineWithTransformation)
   }

   private fun buildTransformStage(
      inputType: QualifiedName?,
      outputTypeName: QualifiedName,
      jetPipelineBuilder: GeneralStage<MessageContentProvider>
   ): GeneralStage<out MessageContentProvider> {
      val jetPipelineWithTransformation = if (inputType != null && inputType != outputTypeName) {
         jetPipelineBuilder.mapUsingServiceAsync(
            VyneTransformationService.serviceFactory()
         ) { transformationService, messageContentProvider ->
            transformationService.transformWithVyne(messageContentProvider, inputType, outputTypeName)
         }
            .setName("Transform ${inputType.shortDisplayName} to ${outputTypeName.shortDisplayName} using Vyne")
      } else {
         jetPipelineBuilder.map { message -> message }
      }
      return jetPipelineWithTransformation
   }

   private fun <O : PipelineTransportSpec> buildSink(
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: O,
      sinkBuilder: PipelineSinkBuilder<O, Any>,
      jetPipelineWithTransformation: GeneralStage<out MessageContentProvider>
   ) {
      if (pipelineTransportSpec is WindowingPipelineTransportSpec) {
         require(sinkBuilder is WindowingPipelineSinkBuilder) { "Output spec is a WindowingPipelineSpec, but sink builder ${sinkBuilder::class.simpleName} does not support windowing" }
         val jetPipelineWithTransformationAsStream =
            if (jetPipelineWithTransformation is StreamStage<out MessageContentProvider>) {
               jetPipelineWithTransformation
            } else {
               jetPipelineWithTransformation.addTimestamps({ System.currentTimeMillis() }, 10000)
            }
         jetPipelineWithTransformationAsStream
            .window(WindowDefinition.tumbling((pipelineTransportSpec as WindowingPipelineTransportSpec).windowDurationMs))
            .aggregate(AggregateOperations.toList())
            .writeTo(sinkBuilder.build(pipelineId, pipelineName, pipelineTransportSpec))
            .setName("Write window of content to ${pipelineTransportSpec.description}")
      } else {
         require(sinkBuilder is SingleMessagePipelineSinkBuilder) { "Output spec is a single message spec, but sink builder ${sinkBuilder::class.simpleName} does not accept single messages" }
         jetPipelineWithTransformation
            .writeTo(sinkBuilder.build(pipelineId, pipelineName, pipelineTransportSpec))
            .setName("Write message to ${pipelineTransportSpec.description}")
      }
   }
}

