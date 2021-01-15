package io.vyne.schemaStore

import arrow.core.Either
import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import lang.taxi.CompilationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.listener.RetryListenerSupport
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


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
            log().warn(
               "Operation {} failed with exception {}, will continue to retry",
               context!!.getAttribute(RETRYABLE_PROCESS_NAME),
               throwable!!.message
            )
         }
      })
      return template
   }
}

class HttpSchemaStoreClient(
   private val schemaStoreService: SchemaStoreService,
   private val eventPublisher: ApplicationEventPublisher,
   @Value("\${vyne.schema.publishRetryInterval:3s}") private val publishRetryInterval: Duration,
   @Value("\${vyne.schema.pollInterval:5s}") private val pollInterval: Duration
) : SchemaStoreClient {

   init {
      log().info("Initializing client vyne.schema.pollInterval=${pollInterval}, vyne.schema.publishRetryInterval=${publishRetryInterval}")
   }

   private val retryTemplate: RetryTemplate = RetryConfig.simpleRetryWithBackoff(publishRetryInterval)
   private var poller: Disposable? = null
   private var schemaSet: SchemaSet = SchemaSet.EMPTY
   private val generationCounter: AtomicInteger = AtomicInteger(0)

   override fun schemaSet() = schemaSet

   override fun validateSchemas(versionedSources: List<VersionedSource>): Either<Pair<CompilationException, List<ParsedSource>>, Pair<Schema, List<ParsedSource>>> {
      TODO("Not yet implemented - Is this used?")
   }

   override fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema> {
      val result: SourceSubmissionResponse =
         retryTemplate.execute<SourceSubmissionResponse, Exception> { context: RetryContext ->
            log().info("Pushing ${versionedSources.size} schemas to store ${versionedSources.map { it.name }}")
            schemaStoreService.submitSources(versionedSources)
         }

      if (result.isValid) {
         return Either.right(result.schemaSet.schema)
      }

      return Either.left(CompilationException(result.errors))
   }

   override val generation: Int
      get() {
         return generationCounter.get()
      }


   @PostConstruct
   fun startPolling() {
      poller = Flux.interval(pollInterval, Schedulers.newSingle("HttpSchemaStorePoller"))
         .doOnNext { pollForSchemaUpdates() }
         .subscribe()
   }

   @PreDestroy
   fun stopPolling() {
      poller?.dispose()
   }

   fun pollForSchemaUpdates() {
      try {
         val schemaSet = schemaStoreService.listSchemas()
         SchemaSetChangedEvent.generateFor(this.schemaSet, schemaSet)?.let { event ->
            this.schemaSet = schemaSet
            this.generationCounter.incrementAndGet()
            log().info("Updated to SchemaSet ${schemaSet.id}, generation $generation, ${schemaSet.size()} schemas, ${schemaSet.sources.map { it.source.id }}")
            eventPublisher.publishEvent(event)
         }
      } catch (e: Exception) {
         log().warn("Failed to fetch schemas: $e")
      }
   }
}
