package com.orbitalhq.pipelines.jet.streams

import com.google.common.cache.CacheBuilder
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.pipelines.jet.api.transport.hazelcast.HazelcastTopicSinkSpec
import com.orbitalhq.schemas.fqn
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * Service which exposes results from persistent streams over RSocket on request.
 *
 * Orbital's main service subscribes here to publish results over HTTP
 */
@Controller
class StreamResultsService(
   private val hazelcastInstance: HazelcastInstance,
) {
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
   fun getResultFeed(@DestinationVariable("streamName") streamName: String): Flux<Any> {
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
