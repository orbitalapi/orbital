package io.vyne.pipelines.runner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.Vyne
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.pipelines.JacksonContentProvider
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineMessage
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.RawPipelineMessage
import io.vyne.pipelines.TransformablePipelineMessage
import io.vyne.pipelines.runner.events.ObserverProvider
import io.vyne.pipelines.runner.events.PipelineStageObserver
import io.vyne.pipelines.runner.events.PipelineStageObserverProvider
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.spring.VyneProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

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
      val input = observer.catchAndLog("Failed to create pipeline input") {
         transportFactory.buildInput(
            pipeline.input.transport,
            inputObserver,
            pipeline
         )
      }
      val output = observer.catchAndLog("Failed to create pipeline output") {
         transportFactory.buildOutput(
            pipeline.output.transport,
            outputObserver,
            pipeline
         )
      }
      val inputType =
         observer.catchAndLog("Failed to resolve input type") { input.type(vyne.schema) }
      val outputType =
         observer.catchAndLog("Failed to resolve output type") { output.type(vyne.schema) }

      val instancesFeed = input.feed
         .name("pipeline_ingestion_request")
         .tag("pipeline_name", pipeline.name)
         .metrics()
         .flatMap { inputMessage -> ingest(inputMessage, inputType, outputType, pipeline, vyne) }
         .flatMap { (observerProvider, message) -> transform(observerProvider, message, vyne) }
         .flatMap { (observerProvider, message) ->
            val destination = if (message.overrideOutput != null) {
               observer.info { "Destination changed to ${message.overrideOutput!!.description}" }
               message.overrideOutput!!
            } else {
               output
            }
            publish(observerProvider, message, destination, vyne.schema)
         }.onErrorResume {
            Mono.empty()
         }

      return PipelineInstance(
         pipeline,
         instancesFeed,
         Instant.now(),
         input,
         output
      )
   }

   private fun ingest(
      message: PipelineInputMessage,
      inputType: Type,
      outputType: Type,
      pipeline: Pipeline,
      vyne: Vyne
   ): Mono<Pair<PipelineStageObserverProvider, PipelineMessage>> {
      val stageObserverProvider: PipelineStageObserverProvider = observerProvider.pipelineObserver(
         pipeline,
         message
      )
      val logger = stageObserverProvider("Ingest")

      val pipelineMessage = when (inputType == outputType) {
         true -> RawPipelineMessage(message.contentProvider, pipeline, inputType, outputType, message.overrideOutput)
         false -> {
            val typedInstance = message.contentProvider.readAsTypedInstance(logger, inputType, vyne.schema)
            TransformablePipelineMessage(
               message.contentProvider,
               pipeline,
               inputType,
               outputType,
               typedInstance,
               overrideOutput = message.overrideOutput
            )
         }
      }

      return loggedMono(logger) {
         stageObserverProvider to pipelineMessage
      }
   }

   private fun transform(
      observerProvider: PipelineStageObserverProvider,
      pipelineMessage: PipelineMessage,
      vyne: Vyne
   ): Mono<Pair<PipelineStageObserverProvider, PipelineMessage>> {
      val logger = observerProvider("Transform")

      // Transform if needed
      val transformedMessage = when (pipelineMessage) {
         is TransformablePipelineMessage -> pipelineMessage.copy(
            transformedInstance = vyneTransformation(
               pipelineMessage,
               pipelineMessage.outputType,
               vyne
            )
         )
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

      // TODO : this needs thinking about.
      // The scenario we're hacking here is that if we recieved a collection (eg., from a service),
      // and we're pushing to as cask, we should transform A[] -> B[].
      // However, this is a pretty brute-force place to apply the logic, and could well
      // cause issues down the line.
      // An alternative would be to handle this somewhere in the cask spec, but that feels equally as messy.
      val vyneTransformationType = if (message.instance is TypedCollection && !outputType.isCollection) {
         outputType.asArrayType()
      } else {
         outputType
      }

      // Transform
      val transformedResults =  runBlocking {
         vyne.query(queryId = UUID.randomUUID().toString()).addFact(message.instance)
            .build(vyneTransformationType.name).results
            .toList()
      }

      return when {
         transformedResults.isEmpty() -> error("Conversion failed")
         transformedResults.size == 1 -> transformedResults.first()
         else -> TypedCollection.from(transformedResults)
      }

   }

   private fun publish(
      observerProvider: PipelineStageObserverProvider,
      message: PipelineMessage,
      output: PipelineOutputTransport,
      schema: Schema
   ): Mono<PipelineMessage> {
      val logger = observerProvider("Publish")
      return loggedMono(logger) {
         val outputMessage: MessageContentProvider = when (message) {
            is TransformablePipelineMessage -> JacksonContentProvider(
               objectMapper,
               message.transformedInstance!!.toRawObject()!!
            )
            is RawPipelineMessage -> message.content
         }

         output.write(outputMessage, logger, schema)

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

