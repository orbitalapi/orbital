package io.vyne.schema.publisher.rsocket

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.metadata.TaggingMetadataCodec
import io.rsocket.util.DefaultPayload
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schema.publisher.*
import io.vyne.schema.rsocket.CBORJackson
import io.vyne.schema.rsocket.RSocketRoutes
import io.vyne.schema.rsocket.SchemaUpdatesRSocketFactory
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.toMessage
import lang.taxi.CompilationException
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.time.Duration


class RSocketSchemaPublisher(
   private val publisherId: String,
   private val rsocketFactory: SchemaUpdatesRSocketFactory,
   private val objectMapper: ObjectMapper = CBORJackson.defaultMapper
) : AsyncSchemaPublisher {
   private val logger = KotlinLogging.logger {}

   private val publisherMetadata = PublisherConfiguration(
      publisherId,
      RSocketKeepAlive
   )

   private val rsocketSupplier: Mono<RSocket> = rsocketFactory.build().cache()
   override fun submitSchemasAsync(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId>
   ): Mono<Either<CompilationException, Schema>> {
      logger.info("Pushing ${versionedSources.size} schemas to store ${versionedSources.map { it.name }}")

      return rsocketSupplier.flatMap { rsocket ->
         logger.debug { "Retrieved connected rsocket" }
         // https://stackoverflow.com/a/62776146
         val submission = VersionedSourceSubmission(
            versionedSources,
            publisherMetadata
         )
         val messageData = encodeMessage(submission)
         val metadata = RSocketRoutes.schemaSubmissionRouteMetadata()
         val response = rsocket.requestResponse(
            DefaultPayload.create(
               messageData, metadata
            )
         ).map { response ->
            logger.debug { "Received response from schema submission" }
            val result = objectMapper.readValue<SourceSubmissionResponse>(response.data().array())
            if (result.isValid) {
               logger.info { "Submitted schema successfully, confirmed as valid" }
            } else {
               logger.warn { "Schema was rejected by server: ${result.errors.toMessage()}" }
            }
            result.asEither()
         }
         response
      }
   }

   private fun encodeMessage(message: Any): ByteBuf {
      return ByteBufAllocator.DEFAULT.buffer().writeBytes(objectMapper.writeValueAsBytes(message))
   }

}


//class RSocketSchemaPublisher(
//   private val publisherConfiguration: PublisherConfiguration,
//   rSocketSchemaServerProxy: RSocketSchemaServerProxy,
//   private val publishRetryInterval: Duration
//) :
//   SchemaPublisher {
//   private val publishRetry: RetryBackoffSpec = Retry.backoff(Long.MAX_VALUE, publishRetryInterval)
//   private val schemaServerConnectionDisconnectedSink = Sinks.many().replay().latest<Unit>()
//   override val schemaServerConnectionLost: Publisher<Unit> = schemaServerConnectionDisconnectedSink.asFlux()
//   private val requesterMono = rSocketSchemaServerProxy.schemaServerPublishSchemaConnection(
//      publisherConfiguration,
//      schemaServerConnectionDisconnectedSink
//   )
//
//   override fun submitSchemas(
//      versionedSources: List<VersionedSource>,
//      removedSources: List<SchemaId>
//   ): Either<CompilationException, Schema> {
//      val submission = VersionedSourceSubmission(
//         versionedSources,
//         publisherConfiguration
//      )
//      logger.info("Pushing ${versionedSources.size} schemas to store ${versionedSources.map { it.name }}")
//      val result: SourceSubmissionResponse = requesterMono.flatMap { requester ->
//         requester.route("request.vyneSchemaSubmission").data(submission)
//            .retrieveMono(SourceSubmissionResponse::class.java)
//      }.retryWhen(publishRetry
//         .doBeforeRetry { retrySignal: Retry.RetrySignal? ->
//            logger.warn { "Error when submitting schema to schema server - retrying {$retrySignal}" }
//         })
//         .block()!!
//      return result.mapTo()
//   }
//}
