package io.vyne.schemaStore.eureka

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
import io.vyne.utils.xtimed
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.nio.charset.Charset
import javax.inject.Provider

internal data class SourcePublisherRegistration(
   val applicationName: String,
   // We make this a list, as even though publisher services only expose a single endpoint,
   // if there are multiple instances of the service running, we end up with mulitple endpoints to poll
   val sourceUrls: List<String>,
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
   private val restTemplate: RestTemplate = RestTemplate()) : SchemaStore {

   private var sources = mutableListOf<SourcePublisherRegistration>()
   private val client = clientProvider.get()
   private val eurekaNotificationUpdater = EurekaNotificationServerListUpdater(clientProvider)

   init {
      // This is executed on a dedicated ThreadPool and it is guaranteed that 'only' one callback is active for the given 'event'
      eurekaNotificationUpdater.start {
         // Design note: previously we checked the apps.version from Eureka to see if things
         // had changed.  However, we're seeing missed changes, and that method is deprecated,
         // it's possible that it served as a premature optimisation.
         // Let's add it back if this stuff turns out to be expensive
         log().debug("Received a eureka event, checking for changes to sources")

         val currentSourceSet = rebuildSources()
         val delta = calculateDelta(sources, currentSourceSet)
         if (delta.hasChanges) {
            log().info("Found changes to schema, proceeding to update.")
            val logMsg = currentSourceSet.map {
               "${it.applicationName} exposes ${it.availableSources.size} sources (@${it.sourceHash}) at ${it.sourceUrls}"
            }
            log().info("Sources Summary: $logMsg")
            updateSources(currentSourceSet, delta)
         } else {
            log().debug("No changes found, nothing to do")
         }
      }
   }

   private fun updateSources(currentSourceSet: List<SourcePublisherRegistration>, delta: SourceDelta) {
      fun sourcesDescription(sources: List<SourcePublisherRegistration>): String = sources.joinToString("\n") { "${it.applicationName} @${it.sourceHash}" }
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
         eventPublisher.publishEvent(SchemaSetChangedEvent(oldSchemaSet, this.schemaSet()))
      } else {
         log().info("No changes found to sources - nothing to do")
      }
   }

   private fun sync(delta: SourceDelta) {
      val newSources = delta.newSources.map { publisherRegistration -> publisherRegistration to loadSources(publisherRegistration) }.toMap()
      val updatedSources = delta.changedSources.map { publisherRegistration -> publisherRegistration to loadSources(publisherRegistration) }.toMap()
      val modifications: Map<SourcePublisherRegistration, List<VersionedSource>> = newSources + updatedSources
      val modifiedSources = modifications.flatMap { it.value }

      if (modifiedSources.isNotEmpty()) {
         schemaStore.submitSchemas(modifiedSources)
      }

      if (delta.sourceIdsToRemove.isNotEmpty()) {
         schemaStore.removeSourceAndRecompile(delta.sourceIdsToRemove)
      }

      delta.newSources.forEach { publisherRegistration ->
         val loadedSources = newSources[publisherRegistration]
         if (loadedSources.isNullOrEmpty()) {
            log().warn("Not registering new sources for ${publisherRegistration.applicationName} as they failed to be loaded")
         } else {
            this.sources.add(publisherRegistration)
         }
      }
      delta.changedSources.forEach { publisherRegistration ->
         val loadedSources = updatedSources[publisherRegistration]
         if (loadedSources.isNullOrEmpty()) {
            log().warn("Not registering updated sources for ${publisherRegistration.applicationName} as they failed to be loaded")
         } else {
            this.sources.removeIf { it.applicationName == publisherRegistration.applicationName }
            this.sources.add(publisherRegistration)
         }
      }

      this.sources.removeIf { existingSource -> delta.removedSources.any { it.applicationName == existingSource.applicationName } }

      log().info("After applying source deltas, schema is now ${schemaStore.schemaSet()}")
   }

   private fun loadSources(registration: SourcePublisherRegistration): List<VersionedSource> {
      val sources = registration.sourceUrls
         .asSequence()
         .mapIndexedNotNull { index, sourceUrl ->
            try {
               log().info("Attempting to load sources for ${registration.applicationName} from $sourceUrl (${index + 1} of ${registration.sourceUrls.size} possible urls)")
               val result = restTemplate.exchange<List<VersionedSource>>(sourceUrl, HttpMethod.GET, null)
               if (result.statusCode.is2xxSuccessful) {
                  val response = result.body!!
                  log().info("Successfully loaded ${response.size} for ${registration.applicationName} from $sourceUrl")
                  response
               } else {
                  log().warn("Failed to load taxi sources from $sourceUrl - received HTTP Response ${result.statusCode}")
                  null
               }
            } catch (exception: Exception) {
               log().warn("Failed to load taxi sources from $sourceUrl - exception thrown", exception)
               null
            }
         }.firstOrNull()
      if (sources == null) {
         log().error("Failed to load sources for ${registration.applicationName} from any of the ${registration.sourceUrls.size} possible urls")
      }
      return sources ?: emptyList()
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
      return xtimed("Rebuild of Eureka source list") {
//         log().info("Registered Eureka Application ${client.applications.registeredApplications.map { it.name }}")
         client.applications.registeredApplications
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
                     instance to SourcePublisherRegistration(
                        application.name,
                        listOf(concatUrlParts(instance.hostName, instance.port, instance.metadata[EurekaMetadata.VYNE_SCHEMA_URL]!!)),
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

   }

   private fun concatUrlParts(ipAddr: String, port: Int, schemaUrlPath: String): String {
      return "http://$ipAddr:$port/${schemaUrlPath.removePrefix("/")}"
   }

   private fun verifyAllInstancesContainTheSameMappings(application: Application, publishedSources: List<Pair<InstanceInfo, SourcePublisherRegistration>>): SourcePublisherRegistration {
      val sourceRegistrationsBySourceHashes = publishedSources.associateBy { it.second.availableSources.hashCode() }
      if (sourceRegistrationsBySourceHashes.size > 1) {
         log().warn("Application ${application.name} has multiple instances that publish different schemas.")
         // TODO : Be more selective abut which we use.
         // We should favor the last registered version if it's known,
         // otherwise, select ... maybe ... highest version?
         // By selecting the first() we're not being picky (or deterministic) at all --
         // it's effectively up to however Eurkea returned these to us.
      }
      // Collect the full list of endpoints we can call
      val allCandidateUrls = publishedSources.flatMap { it.second.sourceUrls }
      return publishedSources.first().second.copy(sourceUrls = allCandidateUrls)
   }

   override fun schemaSet(): SchemaSet {
      return this.schemaStore.schemaSet()
   }

   override val generation: Int
      get() = this.schemaStore.generation
}

