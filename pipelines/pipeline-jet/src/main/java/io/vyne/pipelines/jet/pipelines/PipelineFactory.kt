package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.aggregate.AggregateOperations
import com.hazelcast.jet.pipeline.BatchStage
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
import io.vyne.schemas.QualifiedName
import io.vyne.pipelines.jet.source.PipelineSourceType
import io.vyne.spring.VyneProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class PipelineFactory(
   private val vyneProvider: VyneProvider,
   private val sourceProvider: PipelineSourceProvider = PipelineSourceProvider.default(),
   private val sinkProvider: PipelineSinkProvider = PipelineSinkProvider.default(),
) {
   private val logger = KotlinLogging.logger {}

   fun <I : PipelineTransportSpec, O : PipelineTransportSpec> createJetPipeline(pipelineSpec: PipelineSpec<I, O>): Pipeline {
      logger.info { "Building pipeline ${pipelineSpec.name} from spec  ${pipelineSpec.id} : ${pipelineSpec.description}" }
      val jetPipeline = Pipeline.create()
      val vyne = vyneProvider.createVyne()
      val sourceBuilder = sourceProvider.getPipelineSource(pipelineSpec)
      val sinkBuilder = sinkProvider.getPipelineSink(pipelineSpec)
      val inputType = sourceBuilder.getEmittedType(pipelineSpec, vyne.schema)
      val outputType = sinkBuilder.getRequiredType(pipelineSpec, vyne.schema)

      var jetPipelineBuilder = if (sourceBuilder.sourceType == PipelineSourceType.Stream) {
         jetPipeline
            .readFrom(sourceBuilder.build(pipelineSpec)!!)
            .withIngestionTimestamps()
            .setName("Ingest from ${pipelineSpec.input.description}")
      } else {
         jetPipeline
            .readFrom(sourceBuilder.buildBatch(pipelineSpec)!!)
            .setName("Ingest from ${pipelineSpec.input.description}")
      }

      val jetPipelineWithTransformation = buildTransformStage(inputType, outputType, jetPipelineBuilder)

      buildSink(pipelineSpec, sinkBuilder, jetPipelineWithTransformation)
      return jetPipeline
   }

   private fun buildTransformStage(
      inputType: QualifiedName,
      outputType: QualifiedName,
      jetPipelineBuilder: GeneralStage<MessageContentProvider>
   ): GeneralStage<out MessageContentProvider> {
      val jetPipelineWithTransformation = if (inputType != outputType) {
         jetPipelineBuilder.mapUsingServiceAsync(
            VyneTransformationService.serviceFactory()
         ) { transformationService, messageContentProvider ->
            transformationService.transformWithVyne(messageContentProvider, inputType, outputType)
         }
            .setName("Transform ${inputType.shortDisplayName} to ${outputType.shortDisplayName} using Vyne")
      } else {
         jetPipelineBuilder.map { message -> message }
      }
      return jetPipelineWithTransformation
   }

   private fun <I : PipelineTransportSpec, O : PipelineTransportSpec> buildSink(
      pipelineSpec: PipelineSpec<I, O>,
      sinkBuilder: PipelineSinkBuilder<O, Any>,
      jetPipelineWithTransformation: GeneralStage<out MessageContentProvider>
   ) {
      if (pipelineSpec.output is WindowingPipelineTransportSpec) {
         require(sinkBuilder is WindowingPipelineSinkBuilder) { "Output spec is a WindowingPipelineSpec, but sink builder ${sinkBuilder::class.simpleName} does not support windowing" }
         require(jetPipelineWithTransformation is StreamStage<out MessageContentProvider>) { "Output spec is a WindowingPipelineSpec, but jetPipelineWithTransformation ${jetPipelineWithTransformation::class.simpleName} does not support windowing" }
         jetPipelineWithTransformation
            .window(WindowDefinition.tumbling((pipelineSpec.output as WindowingPipelineTransportSpec).windowDurationMs))
            .aggregate(AggregateOperations.toList())
            .writeTo(sinkBuilder.build(pipelineSpec))
            .setName("Write window of content to ${pipelineSpec.output.description}")
      } else {
         require(sinkBuilder is SingleMessagePipelineSinkBuilder) { "Output spec is a single message spec, but sink builder ${sinkBuilder::class.simpleName} does not accept single messages" }
         jetPipelineWithTransformation
            .writeTo(sinkBuilder.build(pipelineSpec))
            .setName("Write message to ${pipelineSpec.output.description}")
      }
   }


}

