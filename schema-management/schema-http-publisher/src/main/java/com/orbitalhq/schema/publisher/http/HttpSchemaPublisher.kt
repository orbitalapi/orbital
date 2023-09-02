package com.orbitalhq.schema.publisher.http

import arrow.core.Either
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.SourcePackage
import com.orbitalhq.schema.publisher.KeepAlivePackageSubmission
import com.orbitalhq.schema.publisher.PublisherConfiguration
import com.orbitalhq.schema.publisher.SchemaPublisherTransport
import com.orbitalhq.schema.publisher.SourceSubmissionResponse
import com.orbitalhq.schemas.Schema
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

class HttpSchemaPublisher(
   private val httpSchemaSubmitter: HttpSchemaSubmitter,
   @Value("\${vyne.schema.publishRetryInterval:3s}") private val publishRetryInterval: Duration
) :
   SchemaPublisherTransport {
   private val retryTemplate: RetryTemplate = RetryConfig.simpleRetryWithBackoff(publishRetryInterval)

   init {
      logger.info("Initializing client  vyne.schema.publishRetryInterval=${publishRetryInterval}")
   }

   override fun submitPackage(submission: SourcePackage): Either<CompilationException, Schema> {
      val result: SourceSubmissionResponse = retryTemplate.execute<SourceSubmissionResponse, Exception> {
         logger.info("Pushing ${submission.packageMetadata.identifier} with ${submission.sourcesWithPackageIdentifier.size} schemas to store")
         httpSchemaSubmitter
            .submitSources(
               submission
            ).block()
      }

      return result.asEither()
   }

   override fun submitMonitoredPackage(submission: KeepAlivePackageSubmission): Either<CompilationException, Schema> {
      TODO("Not yet implemented")
   }


   override fun removeSchemas(identifiers: List<PackageIdentifier>): Either<CompilationException, Schema> {
      TODO("Not yet implemented")
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
         override fun <T, E : Throwable> onError(
            context: RetryContext?,
            callback: RetryCallback<T, E>?,
            throwable: Throwable?
         ) {
            logger.warn(
               "Operation {} failed with exception {}, will continue to retry", context!!.getAttribute(
                  RETRYABLE_PROCESS_NAME
               ), throwable!!.message
            )
         }
      })
      return template
   }
}
