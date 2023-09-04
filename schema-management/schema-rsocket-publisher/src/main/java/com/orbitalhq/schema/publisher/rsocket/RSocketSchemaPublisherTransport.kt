package com.orbitalhq.schema.publisher.rsocket

import arrow.core.Either
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.rsocket.RSocket
import io.rsocket.util.DefaultPayload
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.SourcePackage
import com.orbitalhq.schema.publisher.*
import com.orbitalhq.schema.rsocket.CBORJackson
import com.orbitalhq.schema.rsocket.ClientTransportAddress
import com.orbitalhq.schema.rsocket.RSocketRoutes
import com.orbitalhq.schema.rsocket.SchemaServerRSocketFactory
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.taxi.toMessage
import com.orbitalhq.utils.Ids
import lang.taxi.CompilationException
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers


class RSocketSchemaPublisherTransport(
   rsocketFactory: SchemaServerRSocketFactory,
   private val objectMapper: ObjectMapper = CBORJackson.defaultMapper
) : AsyncSchemaPublisherTransport {
   constructor(
      address: ClientTransportAddress,
      objectMapper: ObjectMapper = CBORJackson.defaultMapper
   ) : this(
      SchemaServerRSocketFactory(address),
      objectMapper
   )

   private val logger = KotlinLogging.logger {}

   private val rsocketSupplier: Flux<RSocket> = rsocketFactory.rsockets

   private val sourceSubmissionResponseSink = Sinks.many().multicast().onBackpressureBuffer<SourceSubmissionResponse>()

   override val sourceSubmissionResponses = sourceSubmissionResponseSink.asFlux()

   override fun buildKeepAlivePackage(submission: SourcePackage, publisherId: PublisherId): KeepAlivePackageSubmission {
      return super.buildKeepAlivePackage(submission, publisherId)
   }


   override fun removeSchemas(identifiers: List<PackageIdentifier>): Either<CompilationException, Schema> {
      TODO("Not yet implemented")
   }

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
      submission: KeepAlivePackageSubmission
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
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.boundedElastic())
            .subscribe { rsocket ->
               logger.debug { "$publisherId: Retrieved new connected rsocket, will use for schema publication" }
               // https://stackoverflow.com/a/62776146
               val messageData = encodeMessage(submission)
               val metadata = RSocketRoutes.schemaSubmissionRouteMetadata()
               logger.info { "$publisherId: Submitting schema to schema server" }
               rsocket.requestResponse(
                  DefaultPayload.create(
                     messageData, metadata
                  )
               )
                  .publishOn(Schedulers.boundedElastic())
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
   }

   private fun encodeMessage(message: Any): ByteBuf {
      return ByteBufAllocator.DEFAULT.buffer().writeBytes(objectMapper.writeValueAsBytes(message))
   }

}

