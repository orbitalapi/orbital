package com.orbitalhq.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Wrapper class to help us incorrectly using gauges.
 * Gauges in Micrometer are different from normal meters, in that they cannot be
 * accessed by repeatidly calling the meterRegistry - they need to be called once
 * to register a gauge, then mutate the atomic value.
 *
 * If you directly call meterRegistry.gauge() repeatidly, the value is never changed.
 */
class GaugeRegistry(private val meterRegistry: MeterRegistry) {
   private val intGauges = mutableMapOf<GaugeNameAndTags, AtomicInteger>()
   private val longGauges = mutableMapOf<GaugeNameAndTags, AtomicLong>()

   fun int(name: String, tags: List<Tag>): AtomicInteger {
      return intGauges.getOrPut(GaugeNameAndTags(name, tags)) {
         meterRegistry.gauge(name, tags, AtomicInteger(0))!!
      }
   }
   fun long(name: String, tags: List<Tag>): AtomicLong {
      return longGauges.getOrPut(GaugeNameAndTags(name, tags)) {
         meterRegistry.gauge(name, tags, AtomicLong(0))!!
      }
   }
   companion object {
      fun simple():GaugeRegistry {
         return GaugeRegistry(SimpleMeterRegistry())
      }
   }
}

private data class GaugeNameAndTags(
   val name: String,
   val tags: List<Tag>
)
