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
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.*
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
   private var state = INIT to INIT

   // DISPOSABLES from flux
   private lateinit var pipelineDisposable: Disposable
   private val inputHealthDisposable: Disposable
   private val outputHealthDisposable: Disposable

   init {
      inputHealthDisposable = input.health().subscribe { reportStatus(INPUT, it) }
      outputHealthDisposable = output.health().subscribe { reportStatus(OUTPUT, it) }
   }

   private fun reportStatus(direction: PipelineDirection, status: PipelineTransportStatus) {
      log().info("Pipeline transport direction $direction (${(if (direction == INPUT) input else output).javaClass.simpleName}) reported status $status") // FIXME if

      // ENHANCE: this might not be the best place to perform this logic? Consider moving it to PipelineBuilder once we expose pipeline data to the outside world/Eureka
      // ENHANCE: this method might need to be thread safe?

      // Store the direction update <Old status, New status>
      var directionUpdate = when (direction) {
         INPUT -> state.first to status
         OUTPUT -> state.second to status
      }

      // Store the other direction status
      var otherDirectionState = when (direction) {
         INPUT -> state.second
         OUTPUT -> state.first
      }

      when (directionUpdate) {

         // One Transport is UP
         INIT to UP -> {
            if (otherDirectionState == UP) {
               // If the other transport is UP, subscribe to the flux and get data in
               flux.subscribe()
            }
         }

         // One Transport is DOWN, Pause the incoming data
         // ENHANCE: at this stage, the input might not be UP and not pausable ?
         UP to DOWN, INIT to DOWN -> input.pause()

         // One Transport is UP after being DOWN, resume the input and get data in
         DOWN to UP -> {
            if (otherDirectionState == UP) {
               input.resume()
            }
         }

         // One transport is TERMINATED. Can't recover. Nuke everything.
         // ENHANCE: would a matching like (_, TERMINATED) be possible somehow ? Kotlin doesn't seem to handle this for now after a quick search.
         INIT to TERMINATED, UP to TERMINATED, DOWN to TERMINATED -> destroyPipeline()
      }

      // Update the new Pipeline's state
      state = when (direction) {
         INPUT -> status to state.second
         OUTPUT -> state.first to status
      }

      log().info("Pipeline instance status is now [${state.first},${state.second}]")

   }

   /**
    * Destroys a pipeline and unsubscribe to all underlying flux/resources
    */
   private fun destroyPipeline() {
      listOf(
         inputHealthDisposable,
         outputHealthDisposable,
         pipelineDisposable)
         .forEach { it.dispose() }
   }

}
