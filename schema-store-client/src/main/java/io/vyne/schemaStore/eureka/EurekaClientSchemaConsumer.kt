package io.vyne.schemaStore.eureka

import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
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
import io.vyne.utils.timed
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.nio.charset.Charset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Provider

internal data class SourcePublisherRegistration(
   val applicationName: String,
   val sourceUrl: String,
   val availableSources: List<VersionedSourceReference>
) {
   val sourceHash by lazy {
      val hasher = Hashing.sha256().newHasher()
      availableSources.forEach { hasher.putString(it.sourceIdContentHash, Charset.defaultCharset()) }
      hasher.hash().toString().substring(0, 6)
   }
}

internal data class VersionedSourceReference(
   val sourceName: String,
   val sourceVersion: String,
   val sourceContentHash: String
) {
   val sourceId: String = "$sourceName:$sourceVersion"
   val sourceIdContentHash = "$sourceId@$sourceContentHash"
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
   private val restTemplate: RestTemplate = RestTemplate(),
   private val refreshExecutorService: ExecutorService = Executors.newFixedThreadPool(1)) : SchemaStore {

   private var sources = mutableListOf<SourcePublisherRegistration>()
   private val client = clientProvider.get()
   private val eurekaNotificationUpdater = EurekaNotificationServerListUpdater(clientProvider, refreshExecutorService)

   init {
      // This is executed on a dedicated ThreadPool and it is guaranteed that 'only' one callback is active for the given 'event'
      eurekaNotificationUpdater.start {
         try {
            // Design note: previously we checked the apps.version from Eureka to see if things
            // had changed.  However, we're seeing missed changes, and that method is deprecated,
            // it's possible that it served as a premature optimisation.
            // Let's add it back if this stuff turns out to be expensive
            // this whole block is wrapped in try-catch as without it any unhandled exception simply stops
            // eurekaNotificationUpdater getting further eureka updated
            log().debug("Received a eureka event, checking for changes to sources")

            val currentSourceSet = rebuildSources()
            val delta = calculateDelta(sources, currentSourceSet)
            if (delta.hasChanges) {
               log().info("Found changes to schema, proceeding to update.")
               val logMsg = currentSourceSet.map {
                  "${it.applicationName} exposes ${it.availableSources.size} sources (@${it.sourceHash}) at ${it.sourceUrl}"
               }
               log().info("Sources Summary: $logMsg")
               updateSources(currentSourceSet, delta)
            } else {
               log().debug("No changes found, nothing to do")
            }
         } catch (e: Exception) {
            log().error("Error in processing eureka update", e)

         }
      }
   }

   private fun updateSources(currentSourceSet: List<SourcePublisherRegistration>, delta: SourceDelta) {
      detectDuplicateMismatchedSource(currentSourceSet)

      fun sourcesDescription(sources: List<SourcePublisherRegistration>): String = sources.joinToString("\n") { " - ${it.applicationName} @${it.sourceHash} at ${it.sourceUrl} with ${it.availableSources.size} sources" }
      fun logChanges(verb: String, changed: List<SourcePublisherRegistration>) {
         if (changed.isNotEmpty()) {
            log().info("Found $verb sources: \n${sourcesDescription(changed)}")
         }
      }
      if (delta.hasChanges) {
         logChanges("new", delta.newSources)
         logChanges("modified", delta.changedSources)
         logChanges("removed", delta.removedSources)
         val oldSchemaSet = this.schemaSet()
         sync(delta)
         SchemaSetChangedEvent.generateFor(oldSchemaSet, this.schemaSet())?.let {
            eventPublisher.publishEvent(it)
         }
      } else {
         log().info("No changes found to sources - nothing to do")
      }
   }

   private fun detectDuplicateMismatchedSource(currentSourceSet: List<SourcePublisherRegistration>) {
      val applicationsWithDuplicateSchemas = currentSourceSet.groupBy { it.applicationName }
         .filter { (name, sourceRegistrations) -> sourceRegistrations.size > 1 }

      applicationsWithDuplicateSchemas.forEach { (name,registrations) ->
         val hashes = registrations.map { it.sourceHash }
         if (hashes.distinct().size == 1) {
            log().info("Application $name has ${registrations.size} sources registered - (${registrations.joinToString { it.sourceUrl }}) However, all have the same hash, so this is fine")
         } else {
            // TODO : Here, we should be filtering to the most recent
            log().warn("Application $name has ${registrations.size} sources registered - (${registrations.joinToString { it.sourceUrl }}) However, there are multiple different hashes present.  Multiple different schemas for the same application is not supported.  You should remove one.")
         }
      }
   }

   private fun sync(delta: SourceDelta) {
      val newSources = delta.newSources.flatMap { loadSources(it) }
      val updatedSources = delta.changedSources.flatMap { loadSources(it) }
      val modifications = newSources + updatedSources
      if (modifications.isNotEmpty()) {
         schemaStore.submitSchemas(newSources + updatedSources)
      }

      if (delta.sourceIdsToRemove.isNotEmpty()) {
         schemaStore.removeSourceAndRecompile(delta.sourceIdsToRemove)
      }

      if (delta.changedSources.isNotEmpty()) {
         // Handle the following case:
         // given
         // 'FileSchemaServer' publishes two taxi files - a.taxi and b.taxi
         // When
         // a.taxi is deleted.
         // Then
         // delta.changedSources becomes 'non-empty' (i.e. contains FileSchemaServer)
         // and we need the following bit to remove 'a.taxi' from the list of known sources.
         // see EurekaClientSchemaConsumerTest::`can detect removed sources and update whole schema accordingly`
         delta.changedSources.forEach { changedPublisherRegistration ->
            this.sources
               .firstOrNull { existingPublisherRegistration ->
                  existingPublisherRegistration.applicationName == changedPublisherRegistration.applicationName
               }?.let { existingPublisherRegistration ->
                  existingPublisherRegistration
                     .availableSources
                     .filter { existingSource ->
                        changedPublisherRegistration.availableSources.none { it.sourceId == existingSource.sourceId }
                     }.map { it.sourceId }
               }?.let { schemaStore.removeSourceAndRecompile(it) }
         }
      }

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
            log().error("Failed to load taxi sources from ${registration.sourceUrl} - received HTTP Response ${result.statusCode}.  Will ignore this and continue, and try again later")
            emptyList()
         }
      } catch (exception: Exception) {
         log().error("Failed to load taxi sources from ${registration.sourceUrl} - exception thrown.   Will ignore this and continue, and try again later", exception)
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

      val changedSources = currentSourceSet.filter { currentSource ->
         previousKnownSources.any { previousSource ->
            val isChanged = currentSource.applicationName == previousSource.applicationName && currentSource.availableSources.hashCode() != previousSource.availableSources.hashCode()
            isChanged
         }
      }

      return SourceDelta(
         newSources, changedSources, removedSources
      )

   }

   private fun rebuildSources(): List<SourcePublisherRegistration> {
//         log().info("Registered Eureka Application ${client.applications.registeredApplications.map { it.name }}")
         return client.applications.registeredApplications
            // Require that at least one instance is up
            .filter { application -> application.instances.any { it.status == InstanceInfo.InstanceStatus.UP } }
            .mapNotNull { application ->
            val publishedSources = application.instances
               .filter { it.metadata.containsKey(EurekaMetadata.VYNE_SCHEMA_URL) }
               .map { instance ->
                  val sourceReferences =
                     instance.metadata.keys
                        .filter { key -> key.startsWith(EurekaMetadata.VYNE_SOURCE_PREFIX) }
                        .map { key ->
                           val sourceId = EurekaMetadata.fromXML(key.removePrefix(EurekaMetadata.VYNE_SOURCE_PREFIX))
                           val (sourceName, sourceVersion) = VersionedSource.nameAndVersionFromId(sourceId)
                           val sourceHash = instance.metadata[key]!!
                           VersionedSourceReference(sourceName, sourceVersion, sourceHash)
                        }
                  log().trace("detected ${sourceReferences.size} sources from ${instance.appName}")
                  instance to SourcePublisherRegistration(
                     application.name,
                     concatUrlParts(instance.hostName, instance.port, instance.metadata[EurekaMetadata.VYNE_SCHEMA_URL]!!),
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

