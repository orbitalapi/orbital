package com.orbitalhq.pipelines.jet.streams

import com.orbitalhq.pipelines.jet.api.streams.StreamJobStateEvent
import com.orbitalhq.pipelines.jet.api.streams.StreamStateWithJobStates
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/**
 * Publishes to micrometer the status of endpoints.
 * WE publish an aggregate of all states, as well as an individual guage for each stream
 */
@Component
class StreamStateHealthGauge(
   private val meterRegistry: MeterRegistry,
   private val streamStateListener: StreamStateChangeEventListener
) {
   init {
      val allStates = StreamStateWithJobStates.allStates.associateWith { 0 }.toMutableMap()
      streamStateListener.stateUpdates.subscribe { value ->
         // Track total endpoints by state
         value.values.groupBy { it.statusString }
            .forEach { (status, jobsInState) ->
               allStates[status] = jobsInState.size
            }
         allStates.forEach { (jobState,count) ->
            meterRegistry.gauge("orbital.streams.state.$jobState", count)
         }
         value.forEach { (name, jobStates) ->
            val isHealthy = jobStates.streamStatus.state == StreamStatus.State.RUNNING
               && jobStates.jobState?.status == StreamJobStateEvent.JobStatus.RUNNING
            val isHealthyGaugeValue = if (isHealthy) 1 else 0
            meterRegistry.gauge("orbital.streams.health.$name.healthy", isHealthyGaugeValue)
         }
      }
   }
}
