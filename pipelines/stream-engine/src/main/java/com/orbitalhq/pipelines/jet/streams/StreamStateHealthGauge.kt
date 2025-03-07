package com.orbitalhq.pipelines.jet.streams

import com.orbitalhq.pipelines.jet.api.streams.StreamJobStateEvent
import com.orbitalhq.pipelines.jet.api.streams.StreamStateWithJobStates
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

/**
 * Publishes to micrometer the status of endpoints.
 * WE publish an aggregate of all states, as well as an individual guage for each stream
 */
@Component
class StreamStateHealthGauge(
   private val meterRegistry: MeterRegistry,
   private val streamStateListener: StreamStateChangeEventListener
) {
   private val gauges = mutableMapOf<String, AtomicInteger>()

   init {
      streamStateListener.stateUpdates.subscribe { value ->
         val allStates = StreamStateWithJobStates.allStates.associateWith { 0 }.toMutableMap()
         // Track total endpoints by state
         value.values.groupBy { it.statusString }
            .forEach { (status, jobsInState) ->
               allStates[status] = jobsInState.size
            }
         allStates.forEach { (jobState, count) ->
            gauge("orbital.streams.state.$jobState").set(count)
         }
         value.forEach { (name, jobStates) ->
            val isHealthy = jobStates.streamStatus.state == StreamStatus.State.RUNNING
               && jobStates.jobState?.status == StreamJobStateEvent.JobStatus.RUNNING
            val isHealthyGaugeValue = if (isHealthy) 1 else 0
            gauge("orbital.streams.health.$name.healthy").set(isHealthyGaugeValue)
         }
      }
   }

   private fun gauge(name: String): AtomicInteger {
      return gauges.getOrPut(name) {
         meterRegistry.gauge(name, AtomicInteger(0))!!
      }
   }
}
