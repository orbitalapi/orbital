package io.vyne.pipelines.runner

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.models.TypedInstance
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineDirection.INPUT
import io.vyne.pipelines.PipelineDirection.OUTPUT
import io.vyne.pipelines.PipelineInputTransport
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.DOWN
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
import io.vyne.utils.log
import reactor.core.Disposable
import reactor.core.publisher.Flux
import java.time.Instant

class PipelineInstance(
   override val spec: Pipeline,
   private val flux: Flux<TypedInstance>,
   override val startedTimestamp: Instant,
   @JsonIgnore
   val input: PipelineInputTransport,
   @JsonIgnore
   val output: PipelineOutputTransport
) : PipelineInstanceReference {

   /**
    * <INPUT/OUPUT> transport statuses
    */
   var state: Pair<PipelineTransportStatus, PipelineTransportStatus> = DOWN to DOWN

   // DISPOSABLES from flux
   private lateinit var pipelineDisposable: Disposable
   private val inputHealthDisposable: Disposable
   private val outputHealthDisposable: Disposable

   // First implementation. Using a boolean
   // ENHANCE: more logic in reportStatus
   private var isInit = false

   init {
      inputHealthDisposable = input.health().subscribe { reportStatus(INPUT, it) }
      outputHealthDisposable = output.health().subscribe { reportStatus(OUTPUT, it) }
   }

   private fun reportStatus(direction: PipelineDirection, status: PipelineTransportStatus) {
      log().info("Pipeline transport direction $direction (${(if(direction == INPUT) input else output).javaClass.simpleName}) reported status $status") // FIXME if
      state = when (direction) {
         INPUT -> status to state.second
         OUTPUT -> state.first to status
      }

      when (state) {
         UP to UP -> {
            if(!isInit) {
               isInit = true
               pipelineDisposable = flux.subscribe()
            } else {
               log().info("Resuming input ${input.javaClass.simpleName}")
               input.resume()
            }
         }


         UP to DOWN -> {
            if (isInit) {
               log().info("Pausing input ${input.javaClass.simpleName}")
               input.pause()
            }
         }
      }

      log().info("Pipeline instance status is now [${state.first},${state.second}]")

   }


   /**
    * Destroys a pipeline and unsubscribe to all underlying flux/resources
    */
   fun destroyPipeline() {
      listOf(
         inputHealthDisposable,
         outputHealthDisposable,
         pipelineDisposable)
         .forEach { it.dispose() }
   }

}



