package io.vyne.schemaStore

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.vyne.VersionedSource
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class HttpSchemaStore(private val httpVersionedSchemaProvider: HttpVersionedSchemaProvider,
                      private val eventPublisher: ApplicationEventPublisher,
                      @Value("\${vyne.schema.pollInterval:5s}") private val pollInterval: Duration) : SchemaStore {
   private var poller: ScheduledFuture<*>? = null
   private val localValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient()
   @Volatile
   private var poll = true
   override fun schemaSet() = localValidatingSchemaStoreClient.schemaSet()
   override val generation: Int
      get() = localValidatingSchemaStoreClient.generation

   @PostConstruct
   fun startPolling() {
      val scheduledExecutor =  Executors.newSingleThreadScheduledExecutor(ThreadFactoryBuilder().setNameFormat("HttpSchemaStore-%d").build())
      scheduledExecutor.scheduleWithFixedDelay( object : Runnable {
         override fun run() {
            try {
               pollForSchemaUpdates()
            } catch (e: Exception) {
               log().error("Error in fetching the latest schema", e)
            }
         }
      }, 0L, pollInterval.toMillis(), TimeUnit.MILLISECONDS)
   }

   @PreDestroy
   fun stopPolling() {
      poller?.cancel(true)
   }

   @EventListener
   fun controlPoll(event: ControlSchemaPollEvent) {
      log().info("Processing $event")
      this.poll = event.poll
   }

   fun pollForSchemaUpdates() {
      try {
         if (!poll) {
            log().info("skipping poll from query-server as poll flag is false")
            return
         }
         val versionedSources = httpVersionedSchemaProvider.getVersionedSchemas()
         log().trace("pulled ${versionedSources.size} sources from query-server")
         if (shouldRecompile(localValidatingSchemaStoreClient.schemaSet().allSources, versionedSources)) {
            val oldSchemaSet = this.schemaSet()
            localValidatingSchemaStoreClient.submitSchemas(versionedSources)
            SchemaSetChangedEvent.generateFor(oldSchemaSet, this.schemaSet())?.let {
               log().info("Updated to SchemaSet ${schemaSet().id}, generation $generation, ${schemaSet().size()} schemas, ${schemaSet().sources.map { it.source.id }}")
               eventPublisher.publishEvent(it)
            }
         } else {
            log().debug("No Change detected in schema")
         }
      } catch (e: Exception) {
         log().warn("Failed to fetch schemas", e)
      }
   }

   fun shouldRecompile(previousKnownSources: List<VersionedSource>, currentSourceSet: List<VersionedSource>): Boolean {
      val newSources = currentSourceSet.filter { currentSourceRegistration ->
         previousKnownSources.none { previousKnownSource -> previousKnownSource.name == currentSourceRegistration.name }
      }

      val removedSources = previousKnownSources.filter { previousKnownSource ->
         currentSourceSet.none { currentSourceRegistration -> previousKnownSource.name == currentSourceRegistration.name }
      }

      val changedSources = previousKnownSources.filter { previousKnownSource ->
         currentSourceSet.any {
            val hasChanged = it.name == previousKnownSource.name && it.contentHash != previousKnownSource.contentHash
            if (hasChanged) {
               log().info("Source ${it.name} appears to have changed.  Previous content hash was ${previousKnownSource.contentHash}, current content hash is ${it.contentHash}")
            }
            hasChanged
         }
      }

      return newSources.isNotEmpty() || removedSources.isNotEmpty() || changedSources.isNotEmpty()

   }
}

data class ControlSchemaPollEvent(val poll: Boolean)

