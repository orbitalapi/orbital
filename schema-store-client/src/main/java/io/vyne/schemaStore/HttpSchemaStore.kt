package io.vyne.schemaStore

import io.vyne.VersionedSource
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class HttpSchemaStore(private val httpVersionedSchemaProvider: HttpVersionedSchemaProvider,
                      private val eventPublisher: ApplicationEventPublisher,
                      @Value("\${vyne.schema.pollInterval:5s}") private val pollInterval: Duration) : SchemaStore {
   private var poller: Disposable? = null
   private val localValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient()
   override fun schemaSet() = localValidatingSchemaStoreClient.schemaSet()
   override val generation: Int
      get() = localValidatingSchemaStoreClient.generation

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
         val versionedSources = httpVersionedSchemaProvider.getVersionedSchemas()
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
               log().info("Source ${it.name} appears to have changed.  Previous content hash was ${previousKnownSource.content}, current content hash is ${it.contentHash}")
            }
            hasChanged
         }
      }

      return newSources.isNotEmpty() || removedSources.isNotEmpty() || changedSources.isNotEmpty()

   }
}
