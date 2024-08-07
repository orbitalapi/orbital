package com.orbitalhq.pipelines.jet.streams

import com.google.common.cache.CacheBuilder
import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.schema.api.SchemaProvider
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import java.security.Principal

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
   private val hazelcastObserver: HazelcastStreamResultObserver,
   private val authorizationDecorator: ResultStreamAuthorizationDecorator,
   private val schemaProvider: SchemaProvider
) : StreamResultStreamProvider {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   val cacheSize: Long
      get() {
         return hazelcastObserver.cacheSize
      }

   @MessageMapping("stream-results/{streamName}")
   override fun getResultStream(@DestinationVariable("streamName") streamName: String, principal: Principal?): Flux<Any> {
      val rawStream = hazelcastObserver.getResultStream(streamName)
      return if (principal != null && principal is Authentication) {
         authorizationDecorator.applyPolicies(rawStream, streamName, principal, schemaProvider.schema)
      } else {
         rawStream
      }

   }
}
