package io.vyne.pipelines.runner

import io.vyne.Vyne
import io.vyne.models.TypedInstance
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.runner.events.ObserverProvider
import io.vyne.pipelines.runner.events.PipelineStageObserver
import io.vyne.pipelines.runner.events.PipelineStageObserverProvider
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.spring.VyneProvider
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.time.Instant

@Component
class PipelineBuilder(
   private val transportFactory: PipelineTransportFactory,
   private val vyneFactory: VyneProvider,
   private val observerProvider: ObserverProvider
) {

   fun build(pipeline: Pipeline): PipelineInstance {
      val observer = observerProvider.pipelineObserver(pipeline, null).invoke("Preparing pipeline")
      observer.info { "Building pipeline ${pipeline.name}" }
      val vyne = vyneFactory.createVyne()
      val schema = vyne.schema
      // Grab the types early, in case they're not present in Vyne
      val inputType = observer.catchAndLog("Failed to resolve input type ${pipeline.input.type}") { vyne.type(pipeline.input.type) }
      val outputType = observer.catchAndLog("Failed to resolve output type ${pipeline.output.type}") { vyne.type(pipeline.output.type) }
      val input = observer.catchAndLog("Failed to create pipeline input") { transportFactory.buildInput(pipeline.input.transport) }
      val output = observer.catchAndLog("Failed to create pipeline output") { transportFactory.buildOutput(pipeline.output.transport) }

      val disposable = input.feed
         .flatMap { message -> ingest(message, pipeline, schema) }
         .flatMap { pipelineInput -> transform(pipelineInput, vyne, outputType) }
         .flatMap { pipelineInput -> publish(pipelineInput, output) }
         .subscribe()

      return PipelineInstance(
         pipeline,
         disposable,
         Instant.now(),
         input,
         output
      )
   }

   fun ingest(message: PipelineInputMessage, pipeline: Pipeline, schema: Schema): Mono<Pair<PipelineStageObserverProvider, TypedInstance>> {
      val stageObserverProvider: PipelineStageObserverProvider = observerProvider.pipelineObserver(
         pipeline,
         message
      )

      val logger = stageObserverProvider("Ingest")
      return loggedMono(logger) { sink ->
         // Naieve first implementation.
         // Need to leverage the efficient reading we've built for vyne-db module

         // TODO : The idea here is that metadata may provide hints as to whether
         // or not we want to deserailize the message.
         // Note, as I type this, that may be redundant, as the input feed
         // has enough hints to decide that, and is the concerete place to
         // express the decision.
         // For now, just deserialize everything.
         val typedInstance = message.messageProvider(schema, logger)
         sink.success(stageObserverProvider to typedInstance)
      }
   }

   fun transform(pipelineInput: Pair<PipelineStageObserverProvider, TypedInstance>, vyne: Vyne, outputType: Type): Mono<Pair<PipelineStageObserverProvider, TypedInstance>> {
      val (observerProvider, typedInstance) = pipelineInput
      val logger = observerProvider("Transform")
      return loggedMono(logger) { sink ->
         // TODO : Handle failed transformations.
         // Question: Should Pipelines have dead letter or error topics?
         val queryResult = vyne.query()
            .addFact(typedInstance)
            .build(outputType.name)
         val resultInstance = queryResult.get(outputType.fullyQualifiedName) ?: error("Conversion failed")
         sink.success(observerProvider to resultInstance)
      }
   }

   fun publish(pipelineInput: Pair<PipelineStageObserverProvider, TypedInstance>, output: PipelineOutputTransport): Mono<TypedInstance> {
      val (observerProvider, typedInstance) = pipelineInput
      val logger = observerProvider("Publish")
      return loggedMono(logger) { sink ->
         output.write(typedInstance, logger)
         sink.success(typedInstance)
      }
   }

   fun <T> loggedMono(logger: PipelineStageObserver, mono: (MonoSink<T>) -> Unit): Mono<T> {
      return Mono.create<T> { mono(it) }
         .onErrorResume { exception ->
            logger.completedInError(exception)
            Mono.empty()
         }.doOnSuccess {
            logger.completedSuccessfully()
         }
   }


}
