package com.orbitalhq.cockpit.core.monitoring.operations

import com.orbitalhq.metrics.MetricTags
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
         meterRegistry.counter(
            "orbital.operation.invocation.count",
            event.metricTags.micrometerTags() + MetricTags.Operation.of(event.operation.qualifiedName.longDisplayName)
         ).increment()
      }
      eventConsumer.operationEmittedResultEvents.subscribe { event ->
         meterRegistry.counter(
            "orbital.operation.resultEmitted.count",
            event.metricTags.micrometerTags() + MetricTags.Operation.of(event.operation.qualifiedName.longDisplayName)
         ).increment()
      }
      eventConsumer.operationThrewEvents.subscribe { event ->
         meterRegistry.counter(
            "orbital.operation.errors.count",
            event.metricTags.micrometerTags() + MetricTags.Operation.of(event.operation.qualifiedName.longDisplayName)
         ).increment()
      }
   }
}
