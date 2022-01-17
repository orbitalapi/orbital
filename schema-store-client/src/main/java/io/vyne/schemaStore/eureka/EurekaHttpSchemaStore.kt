package io.vyne.schemaStore.eureka

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.vyne.VersionedSource
import io.vyne.httpSchemaConsumer.HttpVersionedSchemaProvider
import io.vyne.schemaApi.ControlSchemaPollEvent
import io.vyne.schemaConsumerApi.SchemaSetChangedEventRepository
import io.vyne.schemaConsumerApi.SchemaStore
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Used in EUREKA schema Distribution Mode in non - Vyne Query Server Schema Consumer applications
 */
@Deprecated("EUREKA Distribution mode replaced with RSOCKET / HTTP based schema consumption / publication mechanisms")
class EurekaHttpSchemaStore(private val httpVersionedSchemaProvider: HttpVersionedSchemaProvider,
                            private val eventPublisher: ApplicationEventPublisher,
                            @Value("\${vyne.schema.pollInterval:5s}") private val pollInterval: Duration) : SchemaSetChangedEventRepository(), SchemaStore {
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
         val versionedSources = httpVersionedSchemaProvider.getVersionedSchemas().block()
         if (versionedSources == null) {
            log().warn("httpVersionedSchemaProvider provided null list of versioned sources!")
            return
         }
         log().trace("pulled ${versionedSources.size} sources from query-server")
         val schemaChange = shouldRecompile(localValidatingSchemaStoreClient.schemaSet().allSources, versionedSources)
         if (schemaChange.shouldRecompile()) {
            val oldSchemaSet = this.schemaSet()
            localValidatingSchemaStoreClient.submitSchemas(versionedSources, schemaChange.removedSources.map { it.id })
            publishSchemaSetChangedEvent(oldSchemaSet, this.schemaSet())
         } else {
            log().debug("No Change detected in schema")
         }
      } catch (e: Exception) {
         log().warn("Failed to fetch schemas", e)
      }
   }

   fun shouldRecompile(previousKnownSources: List<VersionedSource>, currentSourceSet: List<VersionedSource>): SchemaChange {
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

      return SchemaChange(newSources = newSources, updatedSources = changedSources, removedSources = removedSources)
   }
}



data class SchemaChange(val newSources: List<VersionedSource>, val updatedSources: List<VersionedSource>, val removedSources: List<VersionedSource>) {
   fun shouldRecompile() = newSources.isNotEmpty() || removedSources.isNotEmpty() || updatedSources.isNotEmpty()
}
