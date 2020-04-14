package io.vyne.pipelines.runner

import io.vyne.models.TypedInstance
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.runner.events.ObserverProvider
import io.vyne.pipelines.runner.events.PipelineStageObserverProvider
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.spring.VyneProvider
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class PipelineBuilder(
   val transportFactory: PipelineTransportFactory,
   val vyneFactory: VyneProvider,
   val observerProvider: ObserverProvider
) {
   fun build(pipeline: Pipeline): PipelineInstance {
      val vyne = vyneFactory.createVyne()
      // Grab the types early, in case they're not present in Vyne
      val inputType = vyne.type(pipeline.input.type)
      val outputType = vyne.type(pipeline.output.type)
      val input = transportFactory.buildInput(pipeline.input.transport)
      val output = transportFactory.buildOutput(pipeline.output.transport)
      val disposable = input.feed
         .flatMap { message ->
            val stageObserverProvider: PipelineStageObserverProvider = observerProvider.pipelineObserver(
               pipeline,
               message
            )
            val logger = stageObserverProvider("Ingest")
            Mono.create<Pair<PipelineStageObserverProvider, TypedInstance>> { sink ->
               // Naieve first implementation.
               // Need to leverage the efficient reading we've built for vyne-db module

               // TODO : The idea here is that metadata may provide hints as to whether
               // or not we want to deserailize the message.
               // Note, as I type this, that may be redundant, as the input feed
               // has enough hints to decide that, and is the concerete place to
               // express the decision.
               // For now, just deserialize everything.
               sink.success(stageObserverProvider to message.messageProvider(logger))
            }.onErrorResume { exception ->
               logger.completedInError(exception)
               Mono.empty()
            }.doOnSuccess {
               logger.completedSuccessfully()
            }
         }
         .flatMap { pipelineInput ->
            val (observerProvider, typedInstance) = pipelineInput
            val logger = observerProvider("Transform")
            Mono.create<Pair<PipelineStageObserverProvider, TypedInstance>> { sink ->
               // TODO : Handle failed transformations.
               // Question: Should Pipelines have dead letter or error topics?
               val queryResult = vyne.query()
                  .addFact(typedInstance)
                  .build(outputType.name)
               val resultInstance = queryResult.get(outputType.fullyQualifiedName) ?: error("Conversion failed")
               sink.success(observerProvider to resultInstance)
            }.onErrorResume { exception ->
               logger.completedInError(exception)
               Mono.empty()
            }.doOnSuccess {
               logger.completedSuccessfully()
            }
         }
         .flatMap { pipelineInput ->
            val (observerProvider, typedInstance) = pipelineInput
            val logger = observerProvider("Publish")
            Mono.create<TypedInstance> { sink ->
               output.write(typedInstance, logger)
               sink.success(typedInstance)
            }.onErrorResume { exception ->
               logger.completedInError(exception)
               Mono.empty()
            }.doOnSuccess {
               logger.completedSuccessfully()
            }
         }
         .subscribe()

      return PipelineInstance(
         pipeline,
         disposable,
         input,
         output
      )
   }
}
