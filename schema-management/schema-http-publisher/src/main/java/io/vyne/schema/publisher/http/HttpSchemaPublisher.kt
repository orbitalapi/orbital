package io.vyne.schema.publisher.http

import arrow.core.Either
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schema.publisher.PublisherConfiguration
import io.vyne.schema.publisher.SchemaPublisher
import io.vyne.schema.publisher.SourceSubmissionResponse
import io.vyne.schema.publisher.VersionedSourceSubmission
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.listener.RetryListenerSupport
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.time.Duration

private val logger = KotlinLogging.logger {}

class HttpSchemaPublisher(private val publisherConfiguration: PublisherConfiguration,
                          private val httpSchemaSubmitter: HttpSchemaSubmitter,
                          @Value("\${vyne.schema.publishRetryInterval:3s}") private val publishRetryInterval: Duration):
    SchemaPublisher {
   private val retryTemplate: RetryTemplate = RetryConfig.simpleRetryWithBackoff(publishRetryInterval)
   init {
      logger.info("Initializing client  vyne.schema.publishRetryInterval=${publishRetryInterval}")
   }
   override fun submitSchemas(versionedSources: List<VersionedSource>, removedSources: List<SchemaId>): Either<CompilationException, Schema> {
      val result: SourceSubmissionResponse = retryTemplate.execute<SourceSubmissionResponse, Exception> {
         logger.info("Pushing ${versionedSources.size} schemas to store ${versionedSources.map { it.name }}")
         httpSchemaSubmitter
            .submitSources(
               VersionedSourceSubmission(
                  versionedSources,
                  publisherConfiguration)
            ).block()
      }

      return result.asEither()
   }
}

object RetryConfig {
   const val RETRYABLE_PROCESS_NAME = "processName"
   fun simpleRetryWithBackoff(publishRetryInterval: Duration): RetryTemplate {
      val retryPolicy = SimpleRetryPolicy()
      retryPolicy.maxAttempts = Integer.MAX_VALUE

      val backOffPolicy = FixedBackOffPolicy()
      backOffPolicy.backOffPeriod = publishRetryInterval.toMillis()

      val template = RetryTemplate()
      template.setRetryPolicy(retryPolicy)
      template.setBackOffPolicy(backOffPolicy)
      template.registerListener(object : RetryListenerSupport() {
         override fun <T, E : Throwable> onError(context: RetryContext?, callback: RetryCallback<T, E>?, throwable: Throwable?) {
            logger.warn("Operation {} failed with exception {}, will continue to retry", context!!.getAttribute(
               RETRYABLE_PROCESS_NAME
            ), throwable!!.message)
         }
      })
      return template
   }
}
