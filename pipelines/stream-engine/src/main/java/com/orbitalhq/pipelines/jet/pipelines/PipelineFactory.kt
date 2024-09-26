package com.orbitalhq.pipelines.jet.pipelines

import com.hazelcast.jet.aggregate.AggregateOperations
import com.hazelcast.jet.pipeline.GeneralStage
import com.hazelcast.jet.pipeline.Pipeline
import com.hazelcast.jet.pipeline.ServiceFactories.nonSharedService
import com.hazelcast.jet.pipeline.ServiceFactory
import com.hazelcast.jet.pipeline.StreamStage
import com.hazelcast.jet.pipeline.WindowDefinition
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import com.orbitalhq.VyneClientWithSchema
import com.orbitalhq.models.validation.MandatoryFieldNotNull
import com.orbitalhq.models.validation.ValidationRule
import com.orbitalhq.models.validation.failValidationViolationHandler
import com.orbitalhq.models.validation.noOpViolationHandler
import com.orbitalhq.models.validation.validate
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.WindowingPipelineTransportSpec
import com.orbitalhq.pipelines.jet.sink.PipelineSinkBuilder
import com.orbitalhq.pipelines.jet.sink.PipelineSinkProvider
import com.orbitalhq.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import com.orbitalhq.pipelines.jet.sink.WindowingPipelineSinkBuilder
import com.orbitalhq.pipelines.jet.source.PipelineSourceProvider
import com.orbitalhq.pipelines.jet.source.PipelineSourceType
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.Resource
import lang.taxi.query.TaxiQLQueryString
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class PipelineFactory(
   private val vyneClient: VyneClientWithSchema,
   private val sourceProvider: PipelineSourceProvider,
   private val sinkProvider: PipelineSinkProvider,
) {
   fun <I : PipelineTransportSpec, O : PipelineTransportSpec> createJetPipeline(pipelineSpec: PipelineSpec<I, O>): Pipeline {
      val jetPipeline = Pipeline.create()
      val sourceBuilder = sourceProvider.getPipelineSource(pipelineSpec)
      val schema = vyneClient.schema
      val inputTypeName = sourceBuilder.getEmittedType(pipelineSpec, schema)
      val inputType = if (inputTypeName != null) schema.type(inputTypeName) else null

      val jetPipelineBuilder = if (sourceBuilder.sourceType == PipelineSourceType.Stream) {
         // Looks like the name set we here is used by hazelcast metric publisher which throws
         // com.hazelcast.internal.metrics.impl.LongWordException when the name exceeds 255 chars.
         val name = "ingest-${pipelineSpec.name}".take(255)
         jetPipeline
            .readFrom(sourceBuilder.build(pipelineSpec, inputType)!!)
            .withIngestionTimestamps()
            .setName(name)
      } else {
         jetPipeline.readFrom(sourceBuilder.buildBatch(pipelineSpec, inputType)!!)
            .setName("Ingest from ${pipelineSpec.input.description.take(255)}")
      }


      pipelineSpec.outputs.forEach { output ->
         buildTransformAndSinkStageForOutput(
            inputTypeName,
            schema,
            pipelineSpec.id,
            pipelineSpec.name,
            output,
            jetPipelineBuilder,
            pipelineSpec.transformation
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
      jetPipelineBuilder: GeneralStage<out MessageContentProvider>,
      transformation: TaxiQLQueryString?
   ) {
      val sinkBuilder = sinkProvider.getPipelineSink(pipelineTransportSpec)
      val outputTypeName = sinkBuilder.getRequiredType(pipelineTransportSpec, schema)
      val jetPipelineWithValidation = buildValidationStage(inputType, jetPipelineBuilder, pipelineName)
      val jetPipelineWithTransformation =
         buildTransformStage(inputType, outputTypeName, jetPipelineWithValidation, transformation)
      buildSink(pipelineId, pipelineName, pipelineTransportSpec, sinkBuilder, jetPipelineWithTransformation)
   }

   private fun buildTransformStage(
      inputType: QualifiedName?,
      outputTypeName: QualifiedName?,
      jetPipelineBuilder: GeneralStage<out MessageContentProvider>,
      transformation: TaxiQLQueryString?
   ): GeneralStage<out MessageContentProvider> {
//      if (outputTypeName == null) {
//         require(transformation != null) { "If the output type is not provided, then a transformation must be provided" }
//      }

      val jetPipelineWithTransformation =
         if (inputType != null && (inputType != outputTypeName || transformation != null)) {
            val stage =
               jetPipelineBuilder.mapUsingServiceAsync(VyneTransformationService.serviceFactory()) { transformationService, messageContentProvider ->
                  transformationService.transformWithVyne(
                     messageContentProvider,
                     inputType,
                     outputTypeName,
                     transformation
                  )
               }
            if (transformation == null) {
               stage.setName("Transform ${inputType.shortDisplayName} to ${outputTypeName!!.shortDisplayName}")
            } else {
               stage.setName("Transform ${inputType.shortDisplayName} using a TaxiQL query")
            }

         } else {
            jetPipelineBuilder.map { message -> message }
         }
      return jetPipelineWithTransformation
   }

   private fun buildValidationStage(
      inputType: QualifiedName?, jetPipelineBuilder: GeneralStage<out MessageContentProvider>, pipelineName: String
   ): GeneralStage<out MessageContentProvider> {
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
         context.messageCount.increment()
         if (context.inputType == null) {
            return@filterUsingService true
         }

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
      }.setName("validate-input-$pipelineName")
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
         jetPipelineWithTransformation
            .writeTo(sinkBuilder.build(pipelineId, pipelineName, pipelineTransportSpec))
            .setName("$pipelineName-${pipelineTransportSpec.description}")

      }
   }
}

@SpringAware
data class ValidationFilterContext(
   val logger: ILogger,
   // InputType is null if we're not doing any input validation.
   // Typically, this is when this is a query executed by Orbital, and what we're getting
   // is the result, so no further validation is required,
   val inputType: QualifiedName?,
) : Serializable {
   @Resource
   lateinit var vyneClient: VyneClientWithSchema

   @Resource
   lateinit var meterRegistry: MeterRegistry

   lateinit var messageCount: Counter
   lateinit var processedCounter: Counter
   lateinit var validationFailedCounter: Counter

   fun schema(): Schema {
      return vyneClient.schema
   }

   fun createMetricCounters(pipelineName: String) {
      messageCount = Counter
         .builder("orbital.pipelines.received")
         .tag("queryStream", pipelineName)
         .baseUnit("items")
         .description("Count of items received as inputs to the pipeline.")
         .register(meterRegistry)

      processedCounter = Counter
         .builder("orbital.pipelines.processed")
         .tag("queryStream", pipelineName)
         .baseUnit("items")
         .description("Count of items processed successfully as part of the pipeline execution.")
         .register(meterRegistry)

      validationFailedCounter = Counter
         .builder("orbital.pipelines.validationFailed")
         .tag("queryStream", pipelineName)
         .baseUnit("items")
         .description("Count of items for which the validation failed as part of the pipeline execution.")
         .register(meterRegistry)
   }
}
