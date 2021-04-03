package io.vyne.pipelines.runner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.Vyne
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.pipelines.*
import io.vyne.pipelines.runner.events.ObserverProvider
import io.vyne.pipelines.runner.events.PipelineStageObserver
import io.vyne.pipelines.runner.events.PipelineStageObserverProvider
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.schemas.Type
import io.vyne.spring.VyneProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
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


   fun build(pipeline: Pipeline): PipelineInstance {
      val vyne = vyneFactory.createVyne()

      val observerProvider = observerProvider.pipelineObserver(pipeline, null)
      val observer = observerProvider("Preparing pipeline")
      observer.info { "Building pipeline ${pipeline.name} [Input = ${pipeline.input.description}, output = ${pipeline.output.description}]" }

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
         .flatMap { ingest(it, inputType, outputType, pipeline, vyne) }
         .flatMap { transform(it, vyne) }
         .flatMap {
            val (_, message) = it
            val destination = if (message.overrideOutput != null) {
               observer.info { "Destination changed to ${message.overrideOutput!!.description}" }
               message.overrideOutput!!
            } else {
               output
            }
            publish(it, destination)
         }

      return PipelineInstance(
         pipeline,
         instancesFeed,
         Instant.now(),
         input,
         output
      )
   }

   private fun ingest(message: PipelineInputMessage, inputType: Type, outputType: Type, pipeline: Pipeline, vyne: Vyne): Mono<Pair<PipelineStageObserverProvider, PipelineMessage>> {
      val stageObserverProvider: PipelineStageObserverProvider = observerProvider.pipelineObserver(
         pipeline,
         message
      )
      val logger = stageObserverProvider("Ingest")

      val pipelineMessage = when (inputType == outputType) {
         true -> RawPipelineMessage(message.contentProvider, pipeline, inputType, outputType, message.overrideOutput)
         false -> {
            val typedInstance = TypedInstance.from(
               inputType,
               objectMapper.readTree(message.contentProvider.asString(logger)),
               vyne.schema,
               source = Provided
            )
            TransformablePipelineMessage(message.contentProvider, pipeline, inputType, outputType, typedInstance, overrideOutput = message.overrideOutput)
         }
      }

      return loggedMono(logger) {
         stageObserverProvider to pipelineMessage
      }
   }

   private fun transform(pipelineInput: Pair<PipelineStageObserverProvider, PipelineMessage>, vyne: Vyne): Mono<Pair<PipelineStageObserverProvider, PipelineMessage>> {
      val (observerProvider, message) = pipelineInput
      val logger = observerProvider("Transform")

      //return loggedMono(logger) {


         // Transform if needed
         val pipelineMessage = pipelineInput.second
         val transformedMessage = when (pipelineMessage) {
            is TransformablePipelineMessage -> pipelineMessage.copy(transformedInstance = vyneTransformation(pipelineMessage, pipelineMessage.outputType, vyne))
            is RawPipelineMessage -> pipelineMessage
         }

         // Send to following steps
      return loggedMono(logger) {
         observerProvider to transformedMessage
      }
      //}
   }

   private fun vyneTransformation(message: TransformablePipelineMessage, outputType: Type, vyne: Vyne): TypedInstance {
      // TODO : The idea here is that metadata may provide hints as to whether
      // or not we want to deserailize the message.
      // Note, as I type this, that may be redundant, as the input feed
      // has enough hints to decide that, and is the concerete place to
      // express the decision.

      // Type input message
      // TODO : Handle failed transformations.
      // Question: Should Pipelines have dead letter or error topics?

      // Transform
      return runBlocking { vyne.query().addFact(message.instance).build(outputType.name).results.firstOrNull() ?: error("Conversion failed") }

   }

   private fun publish(pipelineInput: Pair<PipelineStageObserverProvider, PipelineMessage>, output: PipelineOutputTransport): Mono<PipelineMessage> {
      val (observerProvider, message) = pipelineInput
      val logger = observerProvider("Publish")


      return loggedMono(logger) {
         val outputMessage: MessageContentProvider = when (message) {
            is TransformablePipelineMessage -> JacksonContentProvider(objectMapper, message.transformedInstance!!.toRawObject()!!)
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

