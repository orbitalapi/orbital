package io.vyne.schema.publisher.rsocket

import arrow.core.Either
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schema.publisher.PublisherConfiguration
import io.vyne.schema.publisher.SchemaPublisher
import io.vyne.schema.publisher.SourceSubmissionResponse
import io.vyne.schema.publisher.VersionedSourceSubmission
import io.vyne.schema.rsocket.RSocketSchemaServerProxy
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import mu.KotlinLogging
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Value
import reactor.core.publisher.Sinks
import reactor.util.retry.Retry
import reactor.util.retry.RetryBackoffSpec
import java.time.Duration

private val logger = KotlinLogging.logger {}

class RSocketSchemaPublisher(
   private val publisherConfiguration: PublisherConfiguration,
   rSocketSchemaServerProxy: RSocketSchemaServerProxy,
   private val publishRetryInterval: Duration
) :
   SchemaPublisher {
   private val publishRetry: RetryBackoffSpec = Retry.backoff(Long.MAX_VALUE, publishRetryInterval)
   private val schemaServerConnectionDisconnectedSink = Sinks.many().replay().latest<Unit>()
   override val schemaServerConnectionLost: Publisher<Unit> = schemaServerConnectionDisconnectedSink.asFlux()
   private val requesterMono = rSocketSchemaServerProxy.schemaServerPublishSchemaConnection(
      publisherConfiguration,
      schemaServerConnectionDisconnectedSink
   )

   override fun submitSchemas(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      val submission = VersionedSourceSubmission(
         versionedSources,
         publisherConfiguration
      )
      logger.info("Pushing ${versionedSources.size} schemas to store ${versionedSources.map { it.name }}")
      val result: SourceSubmissionResponse = requesterMono.flatMap { requester ->
         requester.route("request.vyneSchemaSubmission").data(submission)
            .retrieveMono(SourceSubmissionResponse::class.java)
      }.retryWhen(publishRetry
         .doBeforeRetry { retrySignal: Retry.RetrySignal? ->
            logger.warn { "Error when submitting schema to schema server - retrying {$retrySignal}" }
         })
         .block()!!
      return result.mapTo()
   }
}
