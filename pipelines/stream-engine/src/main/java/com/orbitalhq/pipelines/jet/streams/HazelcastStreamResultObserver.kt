package com.orbitalhq.pipelines.jet.streams

import com.google.common.cache.CacheBuilder
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.pipelines.jet.api.transport.hazelcast.HazelcastTopicSinkSpec
import com.orbitalhq.schemas.fqn
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Component
class HazelcastStreamResultObserver(private val hazelcastInstance: HazelcastInstance,) {
   private val resultFeedCache = CacheBuilder.newBuilder()
      .build<String, Flux<Any>>()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   val cacheSize:Long
      get() {
         return resultFeedCache.size()
      }
   fun getResultStream(streamName: String): Flux<Any> {
      return resultFeedCache.get(streamName) {

         logger.debug { "Building RSocket result emitter for stream $streamName" }
         val topic = hazelcastInstance.getReliableTopic<Any>(HazelcastTopicSinkSpec.topicNameForStream(streamName.fqn()))
         // We are using multicast here as there might be multiple subscribers that might want to consume the same end point.
         // Using unicast here blows up the second subscriber to a given streamName.
         val sink = Sinks.many().multicast().onBackpressureBuffer<Any>()

         val subscriptionId = topic.addMessageListener { messageEvent ->
            val messagePayload = messageEvent.messageObject
            sink.tryEmitNext(messagePayload)
         }
         logger.info { "subscribed to HZ topic for stream $streamName, subscription id => $subscriptionId" }
         val flux = sink.asFlux()
            .doFinally { signal ->
               val currentSubscriberCount = sink.currentSubscriberCount()
               logger.debug { "Result flux for stream $streamName destroyed because signal ${signal.name} received, current subscriber count => $currentSubscriberCount" }
               if (currentSubscriberCount == 0) {
                  resultFeedCache.invalidate(streamName)
                  logger.debug { "Removing the Hz topic subscription for $streamName subscription id $subscriptionId" }
                  // We need to unregister our topic listener, otherwise we keep getting data even though the sink is disposed.
                  val messageListenerRemoved =  topic.removeMessageListener(subscriptionId)
                  logger.debug { "result stream listener $subscriptionId removed => $messageListenerRemoved" }
               }
            }
         flux
      }
   }

}
