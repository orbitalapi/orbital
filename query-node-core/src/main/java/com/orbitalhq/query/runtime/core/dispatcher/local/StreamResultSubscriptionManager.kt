package com.orbitalhq.query.runtime.core.dispatcher.local

import com.google.common.cache.CacheBuilder
import com.orbitalhq.http.ServicesConfig
import com.orbitalhq.spring.rsocket.RSocketConnectionFactory
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 *
 */
@Component
class StreamResultSubscriptionManager(
   private val discoveryClient: DiscoveryClient,
   private val rSocketConnectionFactory: RSocketConnectionFactory,
   ) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val streamCache = CacheBuilder.newBuilder()
      .build<String, Flux<Any>>()

   fun getResultStream(streamName: String): Flux<Any> {
      return streamCache.get(streamName) {
         logger.info { "Creating subscription for result stream $streamName " }
         val (resultStream, connectionStatus) = rSocketConnectionFactory.reconnectingRSocket(
            ServicesConfig.STREAM_SERVER_NAME,
            discoveryClient,
            "stream-results/$streamName",
            Mono.empty(),
            Any::class
         )

         resultStream.doFinally { signal ->
            logger.info { "Result stream $streamName completed with signal ${signal.name} - clearing cache" }
            streamCache.invalidate(streamName)
         }
      }
   }

   val activeSubscriptions:Long
      get() {
         return streamCache.size()
      }
}
