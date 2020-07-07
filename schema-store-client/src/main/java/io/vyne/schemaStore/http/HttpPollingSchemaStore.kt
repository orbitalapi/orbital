package io.vyne.schemaStore.http

import io.vyne.schemaStore.SchemaService
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaStore
import io.vyne.utils.log
import org.springframework.retry.support.RetryTemplate
import reactor.core.Disposable
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class HttpPollingSchemaStore(
   val schemaService: SchemaService,
   val retryTemplate: RetryTemplate = RetryConfig.simpleRetryWithBackoff(),
   val pollFrequency: Duration = Duration.ofSeconds(1L)
) : SchemaStore {
   private var poller: Disposable? = null

   private var schemaSet: SchemaSet = SchemaSet.EMPTY

   override fun schemaSet() = schemaSet

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

   override val generation: Int
      get() {
         return this.schemaSet.generation
      }


   fun pollForSchemaUpdates() {
      try {
         val schemaSet = schemaService.listSchemas()
         if (this.schemaSet.id != schemaSet.id) {
            this.schemaSet = schemaSet
            log().info("Updated to schema set ${schemaSet.id}, generation $generation (contains ${schemaSet.size()} schemas)")
         }
      } catch (e: Exception) {
         log().warn("Failed to fetch schemas: $e")
      }
   }
}
