package io.vyne.schema.publisher.rsocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.rsocket.RSocket
import io.rsocket.util.DefaultPayload
import io.vyne.VersionedSource
import io.vyne.schema.publisher.*
import io.vyne.schema.rsocket.CBORJackson
import io.vyne.schema.rsocket.ClientTransportAddress
import io.vyne.schema.rsocket.RSocketRoutes
import io.vyne.schema.rsocket.SchemaServerRSocketFactory
import io.vyne.schemas.taxi.toMessage
import io.vyne.utils.Ids
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks


class RSocketSchemaPublisherTransport(
   rsocketFactory: SchemaServerRSocketFactory,
   private val objectMapper: ObjectMapper = CBORJackson.defaultMapper
) : AsyncSchemaPublisherTransport {
   constructor(address: ClientTransportAddress, objectMapper: ObjectMapper = CBORJackson.defaultMapper) : this(
      SchemaServerRSocketFactory(address),
      objectMapper
   )

   private val logger = KotlinLogging.logger {}

   private val rsocketSupplier: Flux<RSocket> = rsocketFactory.rsockets

   private val sourceSubmissionResponseSink = Sinks.many().multicast().onBackpressureBuffer<SourceSubmissionResponse>()

   override val sourceSubmissionResponses = sourceSubmissionResponseSink.asFlux()

   /**
    * Submits the versioned sources whenever a new
    * rsocket connection is established.
    *
    * The publication continues as along as the returned Flux<>
    * has a subscriber.
    *
    * Callers should cancel their subscription when it's no longer
    * appropriate to publish these sources (eg., they've become outdated)
    *
    */
   override fun submitSchemaOnConnection(
      publisherId: String,
      versionedSources: List<VersionedSource>
   ): Flux<SourceSubmissionResponse> {

      // We need to push the schema whenever the the rsocket connection
      // reconnects (ie., whenever the rsocketSUpplier provides a new rsocket).

      val publisherId = Ids.id("schema-publisher-flux-")
      var isCancelled = false
      return Flux.create<SourceSubmissionResponse> { emitter ->
         rsocketSupplier
            // This shouldn't be neccessary,
            // but it looks like someone isn't unsubscribing properly from this flux.
            // I can't see where though.
            // Whenever someone unsubscribes (there should really be only one subscriber)
            // we flag this as cancelled.
            // Possibly a memory leak here, as the subscription continues on longer than it should.
            // But we don't do any work on the remaining subscription, so hopefully cheap.
            .filter { !isCancelled }
            .subscribe { rsocket ->
               logger.debug { "$publisherId: Retrieved new connected rsocket, will use for schema publication" }
               // https://stackoverflow.com/a/62776146
               val submission = VersionedSourceSubmission(
                  versionedSources,
                  publisherId
               )

               val messageData = encodeMessage(submission)
               val metadata = RSocketRoutes.schemaSubmissionRouteMetadata()
               logger.info { "$publisherId: Submitting schema to schema server" }
               rsocket.requestResponse(
                  DefaultPayload.create(
                     messageData, metadata
                  )
               )
                  .subscribe { responsePayload ->
                     logger.debug { "$publisherId: Received response from schema submission" }
                     val result = objectMapper.readValue<SourceSubmissionResponse>(responsePayload.data().array())
                     if (result.isValid) {
                        logger.info { "$publisherId: Submitted schema successfully, confirmed as valid" }
                     } else {
                        logger.warn { "$publisherId: Schema was rejected by server: ${result.errors.toMessage()}" }
                     }
                     emitter.next(result)
                  }
            }
      }.doOnCancel {
         logger.info { "$publisherId: Subscription cancelled." }
         isCancelled = true
      }

//      return sourceSubmissionResponses
   }

   private fun encodeMessage(message: Any): ByteBuf {
      return ByteBufAllocator.DEFAULT.buffer().writeBytes(objectMapper.writeValueAsBytes(message))
   }

}

