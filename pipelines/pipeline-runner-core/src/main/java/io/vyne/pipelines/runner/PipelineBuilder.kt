package io.vyne.pipelines.runner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.Vyne
import io.vyne.models.TypedInstance
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineOutputTransport
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

   fun build(pipeline: Pipeline): PipelineInstance {
      val observer = observerProvider.pipelineObserver(pipeline, null).invoke("Preparing pipeline")
      observer.info { "Building pipeline ${pipeline.name} [Input = ${pipeline.input.transport.type}, output = ${pipeline.output.transport.type}]" }
      val vyne = vyneFactory.createVyne()
      // Grab the types early, in case they're not present in Vyne
      val inputType = observer.catchAndLog("Failed to resolve input type ${pipeline.input.type}") { vyne.type(pipeline.input.type) }
      val outputType = observer.catchAndLog("Failed to resolve output type ${pipeline.output.type}") { vyne.type(pipeline.output.type) }
      val input = observer.catchAndLog("Failed to create pipeline input") { transportFactory.buildInput(pipeline.input.transport) }
      val output = observer.catchAndLog("Failed to create pipeline output") { transportFactory.buildOutput(pipeline.output.transport) }

      val instancesFeed = input.feed
         .name("pipeline_ingestion_request")
         .tag("pipeline_name", pipeline.name)
         .metrics()
         .flatMap { ingest(it, pipeline) }
         .flatMap { transform(it, vyne, inputType, outputType) }
         .flatMap { publish(it, output) }

      return PipelineInstance(
         pipeline,
         instancesFeed,
         Instant.now(),
         input,
         output
      )
   }

   fun ingest(message: PipelineInputMessage, pipeline: Pipeline): Mono<Pair<PipelineStageObserverProvider, String>> {
      val stageObserverProvider: PipelineStageObserverProvider = observerProvider.pipelineObserver(
         pipeline,
         message
      )


      val logger = stageObserverProvider("Ingest")
      return loggedMono(logger) {


         stageObserverProvider to message.messageProvider(logger)
      }
   }

   fun transform(pipelineInput: Pair<PipelineStageObserverProvider, String>, vyne: Vyne, inputType: Type, outputType: Type): Mono<Pair<PipelineStageObserverProvider, String>> {
      val (observerProvider, message) = pipelineInput
      val logger = observerProvider("Transform")

      return loggedMono(logger) {

         // Transform if needed
         var transformedMessage = if (inputType == outputType) {
            message
         } else {
            vyneTransformation(message, vyne, inputType, outputType)
         }

         // Send to following steps
         observerProvider to transformedMessage
      }
   }

   fun vyneTransformation(message: String, vyne: Vyne, inputType: Type, outputType: Type): String {
      // TODO : The idea here is that metadata may provide hints as to whether
      // or not we want to deserailize the message.
      // Note, as I type this, that may be redundant, as the input feed
      // has enough hints to decide that, and is the concerete place to
      // express the decision.

      // Type input message
      var inputInstance = TypedInstance.from(inputType, objectMapper.readTree(message), vyne.schema)

      // Transform
      val queryResult = vyne.query().addFact(inputInstance).build(outputType.name)
      val outputInstance = queryResult.get(outputType.fullyQualifiedName) ?: error("Conversion failed")

      // TODO : Handle failed transformations.
      // Question: Should Pipelines have dead letter or error topics?

      return objectMapper.writeValueAsString(outputInstance.toRawObject())
   }

   fun publish(pipelineInput: Pair<PipelineStageObserverProvider, String>, output: PipelineOutputTransport): Mono<String> {
      val (observerProvider, message) = pipelineInput
      val logger = observerProvider("Publish")
      return loggedMono(logger) {
         output.write(message, logger)
         message
      }
   }

   fun <T> loggedMono(logger: PipelineStageObserver, action: () -> T): Mono<T> {
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

