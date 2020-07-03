package io.vyne.pipelines.runner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.pipelines.*
import io.vyne.pipelines.runner.events.ObserverProvider
import io.vyne.pipelines.runner.events.PipelineStageObserver
import io.vyne.pipelines.runner.events.PipelineStageObserverProvider
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.schemas.Type
import io.vyne.spring.VyneProvider
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Instant

@Component
class PipelineBuilder(
   private val transportFactory: PipelineTransportFactory,
   private val vyneFactory: VyneProvider,
   private val observerProvider: ObserverProvider,
   private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {

   private val vyne = vyneFactory.createVyne()

   fun build(pipeline: Pipeline): PipelineInstance {
      var observerProvider = observerProvider.pipelineObserver(pipeline, null)
      var observer = observerProvider("Preparing pipeline")
      observer.info { "Building pipeline ${pipeline.name} [Input = ${pipeline.input.transport.type}, output = ${pipeline.output.transport.type}]" }

      val inputObserver = observerProvider("Pipeline Input")
      val outputObserver = observerProvider("Pipeline Output")

      // Grab the types early, in case they're not present in Vyne
      val inputType = observer.catchAndLog("Failed to resolve input type ${pipeline.input.type}") { vyne.type(pipeline.input.type) }
      val outputType = observer.catchAndLog("Failed to resolve output type ${pipeline.output.type}") { vyne.type(pipeline.output.type) }
      val input = observer.catchAndLog("Failed to create pipeline input") { transportFactory.buildInput(pipeline.input.transport, inputObserver) }
      val output = observer.catchAndLog("Failed to create pipeline output") { transportFactory.buildOutput(pipeline.output.transport, outputObserver) }

      val instancesFeed = input.feed
         .name("pipeline_ingestion_request")
         .tag("pipeline_name", pipeline.name)
         .metrics()
         .flatMap { ingest(it, inputType, outputType, pipeline) }
         .flatMap { transform(it) }
         .flatMap { publish(it, output) }

      return PipelineInstance(
         pipeline,
         instancesFeed,
         Instant.now(),
         input,
         output
      )
   }

   private fun ingest(message: PipelineInputMessage, inputType: Type, outputType: Type, pipeline: Pipeline): Mono<Pair<PipelineStageObserverProvider, PipelineMessage>> {
      val stageObserverProvider: PipelineStageObserverProvider = observerProvider.pipelineObserver(
         pipeline,
         message
      )
      val logger = stageObserverProvider("Ingest")

      val inputMessage = message.messageProvider(logger)

      val pipelineMessage = when (inputType == outputType) {
         true -> RawPipelineMessage(inputMessage, pipeline, inputType, outputType)
         false -> {
            val typedInstance = TypedInstance.from(inputType, objectMapper.readTree(inputMessage), vyne.schema, source = Provided)
            TransformablePipelineMessage(inputMessage, pipeline, inputType, outputType, typedInstance)
         }
      }

      return loggedMono(logger) {
         stageObserverProvider to pipelineMessage
      }
   }

   private fun transform(pipelineInput: Pair<PipelineStageObserverProvider, PipelineMessage>): Mono<Pair<PipelineStageObserverProvider, PipelineMessage>> {
      val (observerProvider, message) = pipelineInput
      val logger = observerProvider("Transform")

      return loggedMono(logger) {


         // Transform if needed
         var pipelineMessage = pipelineInput.second
         when (pipelineMessage) {
            is TransformablePipelineMessage -> pipelineMessage.transformedInstance = vyneTransformation(pipelineMessage, pipelineMessage.outputType)
            is RawPipelineMessage -> {
            }
         }

         // Send to following steps
         observerProvider to pipelineMessage
      }
   }

   private fun vyneTransformation(message: TransformablePipelineMessage, outputType: Type): TypedInstance {
      // TODO : The idea here is that metadata may provide hints as to whether
      // or not we want to deserailize the message.
      // Note, as I type this, that may be redundant, as the input feed
      // has enough hints to decide that, and is the concerete place to
      // express the decision.

      // Type input message
      // TODO : Handle failed transformations.
      // Question: Should Pipelines have dead letter or error topics?

      // Transform
      val queryResult = vyne.query().addFact(message.instance).build(outputType.name)
      return queryResult.get(outputType.fullyQualifiedName) ?: error("Conversion failed")

   }

   private fun publish(pipelineInput: Pair<PipelineStageObserverProvider, PipelineMessage>, output: PipelineOutputTransport): Mono<PipelineMessage> {
      val (observerProvider, message) = pipelineInput
      val logger = observerProvider("Publish")


      return loggedMono(logger) {
         val outputMessage = when (message) {
            is TransformablePipelineMessage -> objectMapper.writeValueAsString(message.transformedInstance!!.toRawObject())
            is RawPipelineMessage -> message.content
         }

         output.write(outputMessage, logger)

         message
      }
   }

   private fun <T> loggedMono(logger: PipelineStageObserver, action: () -> T): Mono<T> {
      return Mono.create<T> { sink ->
         sink.success(action.invoke())
      }.onErrorResume { exception ->
         logger.completedInError(exception)
         Mono.empty()
      }.doOnSuccess {
         logger.completedSuccessfully()
      }
   }


}

