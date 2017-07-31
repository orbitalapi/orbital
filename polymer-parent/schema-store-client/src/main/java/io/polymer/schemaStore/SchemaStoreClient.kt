package io.polymer.schemaStore

import io.osmosis.polymer.utils.log
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.listener.RetryListenerSupport
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import reactor.core.Disposable
import reactor.core.publisher.Flux
import java.time.Duration
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

object RetyConfig {
   val RETRYABLE_PROCESS_NAME = "processName"
   fun simpleRetryWithBackoff(): RetryTemplate {
      val retryPolicy = SimpleRetryPolicy()
      retryPolicy.maxAttempts = Integer.MAX_VALUE

      val backOffPolicy = FixedBackOffPolicy()
      backOffPolicy.backOffPeriod = 1500 // 1.5 seconds

      val template = RetryTemplate()
      template.setRetryPolicy(retryPolicy)
      template.setBackOffPolicy(backOffPolicy)
      template.registerListener(object : RetryListenerSupport() {
         override fun <T, E : Throwable> onError(context: RetryContext?, callback: RetryCallback<T, E>?, throwable: Throwable?) {
            log().warn("Operation {} failed with exception {}, will continue to retry", context!!.getAttribute(RETRYABLE_PROCESS_NAME), throwable!!.message)
         }
      })
      return template
   }
}

class SchemaStoreClient(val schemaService: SchemaService, val retryTemplate: RetryTemplate = RetyConfig.simpleRetryWithBackoff(), val pollFrequency: Duration = Duration.ofSeconds(1L)) {

   private var poller: Disposable? = null
   var schemaSet: SchemaSet = SchemaSet.EMPTY
      private set


   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String) {
      retryTemplate.execute<Any, Exception> { context: RetryContext ->
         context.setAttribute(RetyConfig.RETRYABLE_PROCESS_NAME, "Publish schemas")
         log().debug("Submitting schema $schemaName v$schemaVersion")
         schemaService.submitSchema(schema, schemaName, schemaVersion)
         log().debug("Schema $schemaName v$schemaVersion submitted successfully")
      }
   }


   @PostConstruct
   fun startPolling() {
      poller = Flux.interval(pollFrequency)
         .doOnNext { pollForSchemaUpdates() }
         .subscribe()
   }

   @PreDestroy
   fun stopPolling() {
      poller?.dispose()
   }

   fun pollForSchemaUpdates() {
      try {
         val schemaSet = schemaService.listSchemas()
         if (this.schemaSet.id != schemaSet.id) {
            this.schemaSet = schemaSet
            log().info("Updated to schema set ${schemaSet.id} (contains ${schemaSet.size()} schemas)")
         }
      } catch (e: Exception) {
         log().warn("Failed to fetch schemas: $e")
      }
   }
}
