package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.aggregate.AggregateOperations
import com.hazelcast.jet.pipeline.GeneralStage
import com.hazelcast.jet.pipeline.Pipeline
import com.hazelcast.jet.pipeline.ServiceFactories.nonSharedService
import com.hazelcast.jet.pipeline.ServiceFactory
import com.hazelcast.jet.pipeline.StreamStage
import com.hazelcast.jet.pipeline.WindowDefinition
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.VyneClientWithSchema
import io.vyne.models.validation.MandatoryFieldNotNull
import io.vyne.models.validation.ValidationRule
import io.vyne.models.validation.failValidationViolationHandler
import io.vyne.models.validation.noOpViolationHandler
import io.vyne.models.validation.validate
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
import org.springframework.stereotype.Component
import java.io.Serializable
import javax.annotation.Resource

@Component
class PipelineFactory(
   private val vyneClient: VyneClientWithSchema,
   private val sourceProvider: PipelineSourceProvider,
   private val sinkProvider: PipelineSinkProvider
) {
   fun <I : PipelineTransportSpec, O : PipelineTransportSpec> createJetPipeline(pipelineSpec: PipelineSpec<I, O>): Pipeline {
      val jetPipeline = Pipeline.create()
      val sourceBuilder = sourceProvider.getPipelineSource(pipelineSpec)
      val schema = vyneClient.schema
      val inputTypeName = sourceBuilder.getEmittedType(pipelineSpec, schema)
      val inputType = if (inputTypeName != null) schema.type(inputTypeName) else null

      val jetPipelineBuilder = if (sourceBuilder.sourceType == PipelineSourceType.Stream) {
         jetPipeline.readFrom(sourceBuilder.build(pipelineSpec, inputType)!!).withIngestionTimestamps()
            .setName("Ingest from ${pipelineSpec.input.description}")
      } else {
         jetPipeline.readFrom(sourceBuilder.buildBatch(pipelineSpec, inputType)!!)
            .setName("Ingest from ${pipelineSpec.input.description}")
      }

      pipelineSpec.outputs.forEach { output ->
         buildTransformAndSinkStageForOutput(
            inputTypeName, schema, pipelineSpec.id, pipelineSpec.name, output, jetPipelineBuilder
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
      jetPipelineBuilder: GeneralStage<out MessageContentProvider>
   ) {
      val sinkBuilder = sinkProvider.getPipelineSink(pipelineTransportSpec)
      val outputTypeName = sinkBuilder.getRequiredType(pipelineTransportSpec, schema)
      val jetPipelineWithValidation = buildValidationStage(inputType, jetPipelineBuilder, pipelineName)
      val jetPipelineWithTransformation = buildTransformStage(inputType, outputTypeName, jetPipelineWithValidation)
      buildSink(pipelineId, pipelineName, pipelineTransportSpec, sinkBuilder, jetPipelineWithTransformation)
   }

   private fun buildTransformStage(
      inputType: QualifiedName?,
      outputTypeName: QualifiedName,
      jetPipelineBuilder: GeneralStage<out MessageContentProvider>
   ): GeneralStage<out MessageContentProvider> {
      val jetPipelineWithTransformation = if (inputType != null && inputType != outputTypeName) {
         jetPipelineBuilder.mapUsingServiceAsync(
            VyneTransformationService.serviceFactory()
         ) { transformationService, messageContentProvider ->
            transformationService.transformWithVyne(messageContentProvider, inputType, outputTypeName)
         }.setName("Transform ${inputType.shortDisplayName} to ${outputTypeName.shortDisplayName} using Vyne")
      } else {
         jetPipelineBuilder.map { message -> message }
      }
      return jetPipelineWithTransformation
   }

   private fun buildValidationStage(
      inputType: QualifiedName?, jetPipelineBuilder: GeneralStage<out MessageContentProvider>, pipelineName: String
   ): GeneralStage<out MessageContentProvider> {
      if (inputType == null) {
         return jetPipelineBuilder
      }
      val serviceFactory: ServiceFactory<*, ValidationFilterContext> = nonSharedService { context ->
         val validationFilterContext = context.managedContext().initialize(
            ValidationFilterContext(context.logger(), inputType)
         ) as ValidationFilterContext

         validationFilterContext.createMetricCounters(pipelineName)
         validationFilterContext
      }

      return jetPipelineBuilder.filterUsingService(
         serviceFactory
      ) { context, message ->
         val schema = context.schema()
         val typedInstance = message.readAsTypedInstance(schema.type(context.inputType), schema)
         val validationResult = typedInstance.validate(
            ValidationRule(
               MandatoryFieldNotNull, listOf(
                  noOpViolationHandler {
                     context.logger.severe(
                        """Validation of the data read failed due to: $it
                        |The original message was: ${message.asString()} """.trimMargin()
                     )
                  }, failValidationViolationHandler()
               )
            )
         )
         if (validationResult) {
            context.processedCounter.increment()
         } else {
            context.validationFailedCounter.increment()
         }
         return@filterUsingService validationResult
      }.setName("Validate ${inputType.shortDisplayName} has all the mandatory fields populated")
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
         jetPipelineWithTransformationAsStream.window(WindowDefinition.tumbling((pipelineTransportSpec as WindowingPipelineTransportSpec).windowDurationMs))
            .aggregate(AggregateOperations.toList())
            .writeTo(sinkBuilder.build(pipelineId, pipelineName, pipelineTransportSpec))
            .setName("Write window of content to ${pipelineTransportSpec.description}")
      } else {
         require(sinkBuilder is SingleMessagePipelineSinkBuilder) { "Output spec is a single message spec, but sink builder ${sinkBuilder::class.simpleName} does not accept single messages" }
         jetPipelineWithTransformation.writeTo(sinkBuilder.build(pipelineId, pipelineName, pipelineTransportSpec))
            .setName("Write message to ${pipelineTransportSpec.description}")
      }
   }
}

@SpringAware
data class ValidationFilterContext(
   val logger: ILogger,
   val inputType: QualifiedName,
) : Serializable {
   @Resource
   lateinit var vyneClient: VyneClientWithSchema

   @Resource
   lateinit var meterRegistry: MeterRegistry

   lateinit var processedCounter: Counter
   lateinit var validationFailedCounter: Counter

   fun schema(): Schema {
      return vyneClient.schema
   }

   fun createMetricCounters(pipelineName: String) {
      processedCounter = Counter
         .builder("vyne.pipelines.processed")
         .tag("pipeline", pipelineName)
         .baseUnit("items")
         .description("Count of items processed successfully as part of the pipeline execution.")
         .register(meterRegistry)

      validationFailedCounter = Counter
         .builder("vyne.pipelines.validationFailed")
         .tag("pipeline", pipelineName)
         .baseUnit("items")
         .description("Count of items for which the validation failed as part of the pipeline execution.")
         .register(meterRegistry)
   }
}
