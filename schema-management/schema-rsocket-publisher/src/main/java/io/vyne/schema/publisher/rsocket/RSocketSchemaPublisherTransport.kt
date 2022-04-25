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
import mu.KotlinLogging
import reactor.core.publisher.Flux


class RSocketSchemaPublisherTransport(
   rsocketFactory: SchemaServerRSocketFactory,
   private val objectMapper: ObjectMapper = CBORJackson.defaultMapper
) : AsyncSchemaPublisherTransport {
   constructor(address: ClientTransportAddress, objectMapper: ObjectMapper = CBORJackson.defaultMapper) : this(
      SchemaServerRSocketFactory(address),
      objectMapper
   )

   constructor(addresses: List<ClientTransportAddress>, objectMapper: ObjectMapper = CBORJackson.defaultMapper) : this(
      SchemaServerRSocketFactory(addresses),
      objectMapper
   )

   private val logger = KotlinLogging.logger {}

   private fun publisherMetadata(publisherId: String) = PublisherConfiguration(
      publisherId,
      RSocketKeepAlive
   )


   private val rsocketSupplier: Flux<RSocket> = rsocketFactory.rsockets

   /**
    * Submits a schema to the schema server whenever
    * an rsocket connection is estalished.
    *
    * If the connection is dropped, and re-established, then the schemas
    * are resubmitted upon the new connection being established
    */
   override fun submitSchemaOnConnection(
      publisherId: String,
      versionedSources: List<VersionedSource>
   ): Flux<SourceSubmissionResponse> {
      return rsocketSupplier.flatMap { rsocket ->
         logger.debug { "Retrieved new connected rsocket" }
         // https://stackoverflow.com/a/62776146
         val submission = VersionedSourceSubmission(
            versionedSources,
            publisherId
         )


         val messageData = encodeMessage(submission)
         val metadata = RSocketRoutes.schemaSubmissionRouteMetadata()
         val response = rsocket.requestResponse(
            DefaultPayload.create(
               messageData, metadata
            )
         )
            .map { response ->
               logger.debug { "Received response from schema submission" }
               val result = objectMapper.readValue<SourceSubmissionResponse>(response.data().array())
               if (result.isValid) {
                  logger.info { "Submitted schema successfully, confirmed as valid" }
               } else {
                  logger.warn { "Schema was rejected by server: ${result.errors.toMessage()}" }
               }
               result
            }
         response
      }
   }

   private fun encodeMessage(message: Any): ByteBuf {
      return ByteBufAllocator.DEFAULT.buffer().writeBytes(objectMapper.writeValueAsBytes(message))
   }

}

