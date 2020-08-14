package io.vyne.schemaStore.eureka

import com.netflix.appinfo.InstanceInfo
import com.netflix.discovery.EurekaClient
import com.netflix.discovery.shared.Application
import com.netflix.niws.loadbalancer.EurekaNotificationServerListUpdater
import io.vyne.VersionedSource
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import javax.inject.Provider

internal data class SourcePublisherRegistration(
   val applicationName: String,
   val sourceUrl: String,
   val availableSources: List<VersionedSourceReference>
)

internal data class VersionedSourceReference(
   val sourceName: String,
   val sourceVersion: String,
   val sourceContentHash: String
) {
   val sourceId: String = "$sourceName:$sourceVersion"
}

internal data class SourceDelta(
   val newSources: List<SourcePublisherRegistration>,
   val changedSources: List<SourcePublisherRegistration>,
   val removedSources: List<SourcePublisherRegistration>
) {
   val hasChanges = newSources.isNotEmpty() || changedSources.isNotEmpty() || removedSources.isNotEmpty()
   val sourceIdsToRemove = removedSources.flatMap { it.availableSources.map { source -> source.sourceId } }
}


/**
 * Consumes the Vyne VersionedSource metadata that has been published
 * to Eureka by EurekaClientSchemaMetaPublishers, and uses this to build (and
 * keep in sync) a SchemaStore.
 *
 * Services register metadata into eureka indicating their taxi schema url, and the
 * sources they are publishing.
 *
 * This service then maintains a register of which sources it currently holds, and
 * invokes service endpoints to address the delta
 *
 * When a service changes it's schema, the Eureka ApplicationVersion is updated, triggering
 * a refresh.
 * Service schemas are evaluated based on their name, version and content hash to determine "equality"
 *
 */
class EurekaClientSchemaConsumer(
   clientProvider: Provider<EurekaClient>,
   private val schemaStore: LocalValidatingSchemaStoreClient,
   private val eventPublisher: ApplicationEventPublisher,
   private val restTemplate: RestTemplate = RestTemplate()) : SchemaStore {
   private var lastEurekaApplicationVersion: Long = -1

   private var sources = mutableListOf<SourcePublisherRegistration>()
   private val client = clientProvider.get()
   private val eurekaNotificationUpdater = EurekaNotificationServerListUpdater(clientProvider)

   init {
      eurekaNotificationUpdater.start {
         // This is executed on a dedicated ThreadPool and it is guaranteed that 'only' one callback is active for the given 'event'
         if (client.applications.version != lastEurekaApplicationVersion) {
            log().info("Looks like the set of applications at Eureka has changed, will refresh schema cache")
            val currentSourceSet = rebuildSources()
            val logMsg = currentSourceSet.map { "${it.applicationName} exposes ${it.availableSources.size} sources at ${it.sourceUrl}" }
            log().info("Sources Summary: $logMsg")
            updateSources(currentSourceSet)
            this.lastEurekaApplicationVersion = client.applications.version
         } else {
            log().debug("Eureka event ignored because the apps hashcode hasn't changed")
         }
      }
   }

   private fun updateSources(currentSourceSet: List<SourcePublisherRegistration>) {
      val delta = calculateDelta(sources, currentSourceSet)
      if (delta.hasChanges) {
         log().info("Found ${delta.newSources.size} new sources, ${delta.changedSources.size} changed sources and ${delta.removedSources.size} removed sources.  Starting to sync")
         val oldSchemaSet = this.schemaSet()
         sync(delta)
         eventPublisher.publishEvent(SchemaSetChangedEvent(oldSchemaSet, this.schemaSet()))
      } else {
         log().info("No changes found to sources - nothing to do")
      }
   }

   private fun sync(delta: SourceDelta) {
      val newSources = delta.newSources.flatMap { loadSources(it) }
      val updatedSources = delta.changedSources.flatMap { loadSources(it) }
      schemaStore.submitSchemas(newSources + updatedSources)

      schemaStore.removeSourceAndRecompile(delta.sourceIdsToRemove)

      this.sources.addAll(delta.newSources)
      this.sources.replaceAll { existingSource ->
         delta.changedSources.firstOrNull { it.applicationName == existingSource.applicationName } ?: existingSource
      }
      this.sources.removeIf { existingSource -> delta.removedSources.any { it.applicationName == existingSource.applicationName } }

      log().info("After applying source deltas, schema is now ${schemaStore.schemaSet()}")
   }

   private fun loadSources(registration: SourcePublisherRegistration): List<VersionedSource> {
      return try {
         val result = restTemplate.exchange<List<VersionedSource>>(registration.sourceUrl, HttpMethod.GET, null)
         if (result.statusCode.is2xxSuccessful) {
            result.body!!
         } else {
            log().error("Failed to load taxi sources from ${registration.sourceUrl} - received HTTP Response ${result.statusCode}")
            emptyList()
         }
      } catch (exception: Exception) {
         log().error("Failed to load taxi sources from ${registration.sourceUrl} - exception thrown", exception)
         emptyList()
      }

   }

   private fun calculateDelta(previousKnownSources: MutableList<SourcePublisherRegistration>, currentSourceSet: List<SourcePublisherRegistration>): SourceDelta {
      val newSources = currentSourceSet.filter { currentSourceRegistration ->
         previousKnownSources.none { previousKnownSource -> previousKnownSource.applicationName == currentSourceRegistration.applicationName }
      }

      val removedSources = previousKnownSources.filter { previousKnownSource ->
         currentSourceSet.none { currentSourceRegistration -> previousKnownSource.applicationName == currentSourceRegistration.applicationName }
      }

      val changedSources = previousKnownSources.filter { previousKnownSource ->
         currentSourceSet.any { it.applicationName == previousKnownSource.applicationName && it.availableSources.hashCode() != previousKnownSource.availableSources.hashCode() }
      }

      return SourceDelta(
         newSources, changedSources, removedSources
      )

   }

   private fun rebuildSources(): List<SourcePublisherRegistration> {
      return client.applications.registeredApplications.mapNotNull { application ->
         val publishedSources = application.instances
            .filter { it.metadata.containsKey(EurekaMetadata.VYNE_SCHEMA_URL) }
            .map { instance ->
               val sourceReferences =
                  instance.metadata.keys
                     .filter { key -> key.startsWith(EurekaMetadata.VYNE_SOURCE_PREFIX) }
                     .map { key ->
                        val sourceId = key.removePrefix(EurekaMetadata.VYNE_SOURCE_PREFIX)
                        val (sourceName, sourceVersion) = VersionedSource.nameAndVersionFromId(sourceId)
                        val sourceHash = instance.metadata[key]!!
                        VersionedSourceReference(sourceName, sourceVersion, sourceHash)
                     }
               instance to SourcePublisherRegistration(
                  application.name,
                  concatUrlParts(instance.ipAddr, instance.port, instance.metadata[EurekaMetadata.VYNE_SCHEMA_URL]!!),
                  sourceReferences
               )
            }
         if (publishedSources.isEmpty()) {
            null
         } else {
            verifyAllInstancesContainTheSameMappings(application, publishedSources)
         }
      }
   }

   private fun concatUrlParts(ipAddr: String, port: Int, schemaUrlPath: String): String {
      return "http://$ipAddr:$port/${schemaUrlPath.removePrefix("/")}"
   }

   private fun verifyAllInstancesContainTheSameMappings(application: Application, publishedSources: List<Pair<InstanceInfo, SourcePublisherRegistration>>): SourcePublisherRegistration {
      val sourceRegistrationsBySourceHashes = publishedSources.associateBy { it.second.availableSources.hashCode() }
      return if (sourceRegistrationsBySourceHashes.size == 1) {
         publishedSources.first().second
      } else {
         log().warn("Application ${application.name} has multiple instances that publish different schemas.")
         // TODO : Be more selective abut which we use.
         // We should favor the last registered version if it's known,
         // otherwise, select ... maybe ... highest version?
         // By selecting the first() we're not being picky (or deterministic) at all --
         // it's effectively up to however Eurkea returned these to us.
         publishedSources.first().second
      }
      TODO("Not yet implemented")
   }

   override fun schemaSet(): SchemaSet {
      return this.schemaStore.schemaSet()
   }

   override val generation: Int
      get() = this.schemaStore.generation
}

