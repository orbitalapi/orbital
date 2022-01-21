package io.vyne.rSocketSchemaPublisher

import arrow.core.Either
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemaPublisherApi.PublisherConfiguration
import io.vyne.schemaPublisherApi.SchemaPublisher
import io.vyne.schemaPublisherApi.SourceSubmissionResponse
import io.vyne.schemaPublisherApi.VersionedSourceSubmission
import io.vyne.schemas.Schema
import io.vyne.schemeRSocketCommon.RSocketSchemaServerProxy
import lang.taxi.CompilationException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import reactor.util.retry.Retry
import reactor.util.retry.RetryBackoffSpec
import java.time.Duration

private val logger = KotlinLogging.logger {}

class RSocketSchemaPublisher(
   private val publisherConfiguration: PublisherConfiguration,
   rSocketSchemaServerProxy: RSocketSchemaServerProxy,
   @Value("\${vyne.schema.publishRetryInterval:3s}") private val publishRetryInterval: Duration
) : SchemaPublisher {
   private val publishRetry: RetryBackoffSpec = Retry.backoff(Long.MAX_VALUE, publishRetryInterval)
   private val requesterMono = rSocketSchemaServerProxy.schemaServerPublishSchemaConnection(publisherConfiguration)

   override fun submitSchemas(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      val submission = VersionedSourceSubmission(
         versionedSources,
         publisherConfiguration
      )

      return submitSchemaPackage(submission, removedSources)
   }

   override fun submitSchemaPackage(
      sourcePackage: VersionedSourceSubmission,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      logger.info("Pushing ${sourcePackage.sources.size} schemas to store")
      val result: SourceSubmissionResponse = requesterMono.flatMap { requester ->
         requester.route("request.vyneSchemaSubmission").data(sourcePackage)
            .retrieveMono(SourceSubmissionResponse::class.java)
      }.retryWhen(publishRetry
         .doBeforeRetry { retrySignal: Retry.RetrySignal? ->
            logger.warn { "Error when submitting schema to schema server - retrying {$retrySignal}" }
         })
         .block()!!
      return result.asEither()
   }
}
