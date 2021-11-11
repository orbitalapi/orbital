package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.pipeline.Pipeline
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.sink.PipelineSinkProvider
import io.vyne.pipelines.jet.source.PipelineSourceProvider
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

      var jetPipelineBuilder = jetPipeline
         .readFrom(sourceBuilder.build(pipelineSpec))
         .withIngestionTimestamps()
         .setName("Ingest from ${pipelineSpec.input.description}")

      val jetPipelineWithTransformation = if (inputType != outputType) {
         jetPipelineBuilder.mapUsingServiceAsync(
            VyneTransformationService.serviceFactory()
         ) { transformationService , messageContentProvider ->
            transformationService.transformWithVyne(messageContentProvider, inputType, outputType)
         }
            .setName("Transform ${inputType.shortDisplayName} to ${outputType.shortDisplayName} using Vyne")
      } else {
         jetPipelineBuilder.map { message -> message }
      }
      jetPipelineWithTransformation
         .writeTo(sinkBuilder.build(pipelineSpec))
         .setName("Write to ${pipelineSpec.output.description}")
      return jetPipeline
   }


}
