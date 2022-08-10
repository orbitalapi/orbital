package io.vyne.schema.publisher.rsocket

import io.vyne.PackageMetadata
import io.vyne.schema.publisher.KeepAliveStrategy
import io.vyne.schema.publisher.KeepAliveStrategyId
import io.vyne.schema.publisher.KeepAliveStrategyMonitor
import io.vyne.schema.publisher.PublisherConfiguration
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger { }

typealias RSocketConnectionId = String

class RSocketPublisherKeepAliveStrategyMonitor : KeepAliveStrategyMonitor {
   private val sink = Sinks.many().multicast()
      .onBackpressureBuffer<PublisherConfiguration>()

   private val monitoredConfigurations = ConcurrentHashMap<String, PublisherConfiguration>()

   override fun appliesTo(keepAlive: KeepAliveStrategy) = keepAlive.id == KeepAliveStrategyId.RSocket

   override val terminatedInstances: Publisher<PublisherConfiguration> = sink.asFlux()

   override fun monitor(publisherConfiguration: PublisherConfiguration) {
      monitoredConfigurations[publisherConfiguration.publisherId] = publisherConfiguration
   }

   object RetryFailOnSerializeEmitHandler : Sinks.EmitFailureHandler {
      override fun onEmitFailure(signalType: SignalType, emitResult: Sinks.EmitResult) =
         emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED
   }

   fun onSchemaPublisherRSocketConnectionTerminated(publisherConfiguration: PublisherConfiguration) {
      logger.info { "$publisherConfiguration Schema publisher disconnected, triggering the flow to remove schemas published by it." }
      sink.emitNext(publisherConfiguration, RetryFailOnSerializeEmitHandler)
   }

   fun addSchemaToConnection(rsocketId: String, packageMetadata: PackageMetadata) {
      // TODO
   }
}
