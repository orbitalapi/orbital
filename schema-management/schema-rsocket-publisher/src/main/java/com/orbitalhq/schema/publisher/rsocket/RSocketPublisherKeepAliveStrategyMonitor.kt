package com.orbitalhq.schema.publisher.rsocket

import com.orbitalhq.PackageMetadata
import com.orbitalhq.schema.publisher.*
import mu.KotlinLogging
import org.reactivestreams.Publisher
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger { }

typealias RSocketConnectionId = String

class RSocketPublisherKeepAliveStrategyMonitor : KeepAliveStrategyMonitor {
   private val sink = Sinks.many().multicast()
      .onBackpressureBuffer<PublisherHealthUpdateMessage>()

   private val monitoredConfigurations = ConcurrentHashMap<String, PublisherConfiguration>()

   override fun appliesTo(keepAlive: KeepAliveStrategy) = keepAlive.id == KeepAliveStrategyId.RSocket

   override val healthUpdateMessages: Publisher<PublisherHealthUpdateMessage> = sink.asFlux()

   override fun monitor(publisherConfiguration: PublisherConfiguration) {
      monitoredConfigurations[publisherConfiguration.publisherId] = publisherConfiguration
   }

   object RetryFailOnSerializeEmitHandler : Sinks.EmitFailureHandler {
      override fun onEmitFailure(signalType: SignalType, emitResult: Sinks.EmitResult) =
         emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED
   }

   fun onSchemaPublisherRSocketConnectionTerminated(publisherConfiguration: PublisherConfiguration) {
      logger.info { "$publisherConfiguration Schema publisher disconnected, marking it as unhealthy." }
      sink.emitNext(
         PublisherHealthUpdateMessage(
            publisherConfiguration.publisherId,
            PublisherHealth(
               status = PublisherHealth.Status.Unhealthy,
               message = "Schema publisher disconnected"
            )
         ), RetryFailOnSerializeEmitHandler
      )
   }
}