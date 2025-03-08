package com.orbitalhq.pipelines.jet.streams

import com.orbitalhq.metrics.GaugeRegistry
import com.orbitalhq.metrics.MetricTags
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
   private val gaugeRegistry: GaugeRegistry,
   private val streamStateListener: StreamStateChangeEventListener
) {
   init {
      streamStateListener.stateUpdates.subscribe { value ->
         val allStates = StreamStateWithJobStates.allStates.associateWith { 0 }.toMutableMap()
         // Track total endpoints by state
         value.values.groupBy { it.statusString }
            .forEach { (status, jobsInState) ->
               allStates[status] = jobsInState.size
            }
         allStates.forEach { (jobState, count) ->
            gaugeRegistry.int("orbital.streams.state.count", listOf(MetricTags.StreamingJobState.of(jobState))).set(count)
         }
      }
   }
}
