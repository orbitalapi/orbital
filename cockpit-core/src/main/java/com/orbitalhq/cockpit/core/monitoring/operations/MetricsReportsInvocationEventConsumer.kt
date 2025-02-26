package com.orbitalhq.cockpit.core.monitoring.operations

import com.orbitalhq.spring.metrics.micrometerTags
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/**
 * Responsible for consuming the events emitted by the CountingOperationInvokerDecorator
 * out to the MeterRegistry.
 */
@Component
class MetricsReportsInvocationEventConsumer(
   private val meterRegistry: MeterRegistry,
   private val eventConsumer: ReactiveOperationInvocationEventConsumer
) {
   init {
       eventConsumer.operationInvokedEvents.subscribe { event ->
          meterRegistry.counter("orbital.operation.invocation.${event.operation.qualifiedName.longDisplayName}", event.metricTags.micrometerTags()).increment()
       }
      eventConsumer.operationEmittedResultEvents.subscribe { event ->
         meterRegistry.counter("orbital.operation.resultEmitted.${event.operation.qualifiedName.longDisplayName}", event.metricTags.micrometerTags()).increment()
      }
      eventConsumer.operationThrewEvents.subscribe { event ->
         meterRegistry.counter("orbital.operation.errors.${event.operation.qualifiedName.longDisplayName}", event.metricTags.micrometerTags()).increment()
      }
   }
}
