package io.vyne.rSocketSchemaPublisher

import io.vyne.schemaPublisherApi.KeepAliveStrategy
import io.vyne.schemaPublisherApi.KeepAliveStrategyId
import io.vyne.schemaPublisherApi.KeepAliveStrategyMonitor
import io.vyne.schemaPublisherApi.PublisherConfiguration
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger { }

class RSocketPublisherKeepAliveStrategyMonitor: KeepAliveStrategyMonitor {
   private val sink = Sinks.many().multicast()
      .onBackpressureBuffer<PublisherConfiguration>()

   private val monitoredConfigurations = ConcurrentHashMap<String, PublisherConfiguration>()

   override fun appliesTo(keepAlive: KeepAliveStrategy) = keepAlive.id == KeepAliveStrategyId.RSocket

   override val terminatedInstances: Publisher<PublisherConfiguration> =  sink.asFlux()

   override fun monitor(publisherConfiguration: PublisherConfiguration) {
      monitoredConfigurations[publisherConfiguration.publisherId] = publisherConfiguration
   }

   object RetryFailOnSerializeEmitHandler: Sinks.EmitFailureHandler {
      override fun onEmitFailure(signalType: SignalType, emitResult: Sinks.EmitResult) = emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED
   }

   fun onSchemaPublisherRSocketConnectionTerminated(publisherConfiguration: PublisherConfiguration) {
      if (monitoredConfigurations.containsKey(publisherConfiguration.publisherId)) {
         logger.info { "$publisherConfiguration Schema Consumer dropped the connection, triggering the flow to remove schemas published by it." }
         sink.emitNext(publisherConfiguration, RetryFailOnSerializeEmitHandler)
      }
   }
}
