package com.orbitalhq.pipelines.jet.streams

import com.google.common.cache.CacheBuilder
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.pipelines.jet.api.transport.hazelcast.HazelcastTopicSinkSpec
import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.schemas.fqn
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * Service which exposes results from persistent streams over RSocket on request,
 * by connecting to the Hazelcast topic where results are being published.
 *
 * This class pulls double-duty.
 *
 * When running in a standalone StreamEngine instance, it provides an RSocket endpoint
 * for Orbital to connect to.
 * Orbital's main service subscribes here to publish results over HTTP
 *
 * When running in a combined Orbital/Stream server node,
 * this class satisfies the StreamResultStreamProvider interface, directly
 * working with SSE / Websocket subscriptions to consume events. (ie., no RSocket required).
 */
@Controller
class StreamResultsService(
   private val hazelcastInstance: HazelcastInstance,
): StreamResultStreamProvider {
   private val resultFeedCache = CacheBuilder.newBuilder()
      .build<String, Flux<Any>>()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   val cacheSize:Long
      get() {
         return resultFeedCache.size()
      }

   @MessageMapping("stream-results/{streamName}")
   override fun getResultStream(@DestinationVariable("streamName") streamName: String): Flux<Any> {
      return resultFeedCache.get(streamName) {
         logger.debug { "Building RSocket result emitter for stream $streamName" }
         val topic = hazelcastInstance.getTopic<Any>(HazelcastTopicSinkSpec.topicNameForStream(streamName.fqn()))
         val sink = Sinks.many().unicast().onBackpressureBuffer<Any>()

         topic.addMessageListener { messageEvent ->
            val messagePayload = messageEvent.messageObject
            sink.tryEmitNext(messagePayload)
         }
         val flux = sink.asFlux()
            .doFinally { signal ->
               logger.debug { "Result flux for stream $streamName destroyed because signal ${signal.name} received" }
               resultFeedCache.invalidate(streamName)
            }
         flux
      }
   }
}
