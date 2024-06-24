package com.orbitalhq.query.runtime.core.dispatcher.local

import com.google.common.cache.CacheBuilder
import com.orbitalhq.http.ServicesConfig
import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.spring.rsocket.RSocketConnectionFactory
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


/**
 * Publishes the result of a streaming query over RSocket.
 * Used when the stream server is running as a standlone server,
 * and communicating results back to Orbital over RSocket.
 *
 * Not used when the streamServer is running embedded in Orbital
 */
@Deprecated("Not currently used as part of a combined Orbital / StreamServer offering. See StreamResultService instead")
class RSocketStreamResultSubscriptionManager(
   private val discoveryClient: DiscoveryClient,
   private val rSocketConnectionFactory: RSocketConnectionFactory,
   ): StreamResultStreamProvider {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val streamCache = CacheBuilder.newBuilder()
      .build<String, Flux<Any>>()

   override fun getResultStream(streamName: String): Flux<Any> {
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
