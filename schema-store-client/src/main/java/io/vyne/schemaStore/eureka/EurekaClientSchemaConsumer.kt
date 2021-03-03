package io.vyne.schemaStore.eureka

import arrow.core.Either
import arrow.core.right
import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
import com.netflix.appinfo.InstanceInfo
import com.netflix.discovery.EurekaClient
import com.netflix.discovery.shared.Application
import com.netflix.niws.loadbalancer.EurekaNotificationServerListUpdater
import io.vyne.VersionedSource
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import io.vyne.utils.timed
import lang.taxi.CompilationException
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
   val sourceUrls: List<String>,
   val availableSources: List<VersionedSourceReference>
) {
   val nameAndUrl = "$applicationName@[${ sourceUrls.sortedDescending().joinToString(",")}]"
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
   val sourceNamesToRemove = removedSources.flatMap { it.availableSources.map { source -> source.sourceName } }
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
   val schemaStore: LocalValidatingSchemaStoreClient,
   private val eventPublisher: ApplicationEventPublisher,
   private val restTemplate: RestTemplate = RestTemplate(),
   private val refreshExecutorService: ExecutorService = Executors.newFixedThreadPool(1)) : SchemaStore, SchemaPublisher {

   private var sources = mutableListOf<SourcePublisherRegistration>()
   private val client = clientProvider.get()
   private val eurekaNotificationUpdater = EurekaNotificationServerListUpdater(clientProvider, refreshExecutorService)

   private val unhealthySources = mutableSetOf<SourcePublisherRegistration>()

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
            removeUnhealthySourcesNowRemoved(currentSourceSet)

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
         } catch (e: Exception) {
            log().error("Error in processing eureka update", e)

         }
      }
   }

   private fun removeUnhealthySourcesNowRemoved(currentSourceSet: List<SourcePublisherRegistration>) {
      val removedUnhealhtySources = unhealthySources.filter { !currentSourceSet.contains(it) }
      removedUnhealhtySources.forEach {
         log().info("Taxi source registration at ${it.nameAndUrl} was previously registered as unhealthy, and now has been removed from Eureka.  Removing it from the list of unhealthy schemas")
         unhealthySources.remove(it)
      }
      if (removedUnhealhtySources.isNotEmpty()) {
         log().info("After pruning removed unhealthy sources, there are now ${unhealthySources.size} unhealthy sources remaining ${unhealthySources.joinToString { it.nameAndUrl }}")
      }

   }

   private fun updateSources(currentSourceSet: List<SourcePublisherRegistration>, delta: SourceDelta) {
      detectDuplicateMismatchedSource(currentSourceSet)

      fun sourcesDescription(sources: List<SourcePublisherRegistration>): String = sources.joinToString("\n") { " - ${it.applicationName} @${it.sourceHash} at ${it.sourceUrls} with ${it.availableSources.size} sources" }
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

      applicationsWithDuplicateSchemas.forEach { (name, registrations) ->
         val hashes = registrations.map { it.sourceHash }
         if (hashes.distinct().size == 1) {
            log().info("Application $name has ${registrations.size} sources registered - (${registrations.joinToString { it.nameAndUrl }}) However, all have the same hash, so this is fine")
         } else {
            // TODO : Here, we should be filtering to the most recent
            log().warn("Application $name has ${registrations.size} sources registered - (${registrations.joinToString { it.nameAndUrl }}) However, there are multiple different hashes present.  Multiple different schemas for the same application is not supported.  You should remove one.")
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
         schemaStore.removeSourceAndRecompile(delta.sourceNamesToRemove)
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
                        changedPublisherRegistration.availableSources.none { it.sourceName == existingSource.sourceName }
                     }.map { it.sourceName }
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
      fun handleFailure(message: String, index: Int, exception: Exception? = null) {
         log().warn(message)
         if (index + 1 >= registration.sourceUrls.size) {
            log().warn("All source urls for application ${registration.applicationName} have failed to load.  Marking this as unhealthy.  Will continue trying", exception)
            unhealthySources.add(registration)
         }
      }
      return registration.sourceUrls
         .asSequence()
         .mapIndexedNotNull { index, sourceUrl ->
            try {
               log().info("Attmepting to load registration ${registration.applicationName} from $sourceUrl.")
               val result = restTemplate.exchange<List<VersionedSource>>(sourceUrl, HttpMethod.GET, null)
               if (result.statusCode.is2xxSuccessful) {
                  if (unhealthySources.remove(registration)) {
                     log().info("Source registration ${registration.applicationName} has been successfully loaded from $sourceUrl after previously being marked unhealthy.  Now healthy again")
                  } else {
                     log().info("Successfully loaded source registration ${registration.applicationName} from $sourceUrl")
                  }
                  result.body!!
               } else {
                  handleFailure("Failed to load taxi sources for ${registration.applicationName} at $sourceUrl (${index + 1} of ${registration.sourceUrls.size} urls). Received HTTP Response ${result.statusCode}.  Will ignore this and continue, and try again later", index)
                  null
               }
            } catch (exception: Exception) {
               handleFailure("Failed to load taxi sources for ${registration.applicationName} at $sourceUrl (${index + 1} of ${registration.sourceUrls.size} urls). - exception thrown.   Will ignore this and continue, and try again later", index, exception)
               unhealthySources.add(registration)
               null
            }
         }.firstOrNull() ?: emptyList()


   }

   private fun calculateDelta(previousKnownSources: MutableList<SourcePublisherRegistration>, currentSourceSet: List<SourcePublisherRegistration>): SourceDelta {
      val newSources = currentSourceSet.filter { currentSourceRegistration ->
         previousKnownSources.none { previousKnownSource -> previousKnownSource.applicationName == currentSourceRegistration.applicationName }
      }


      val removedSources = previousKnownSources.filter { previousKnownSource ->
         currentSourceSet.none { currentSourceRegistration -> previousKnownSource.applicationName == currentSourceRegistration.applicationName }
      }

      val changedSources = (currentSourceSet.filter { currentSource ->
         previousKnownSources.any { previousSource ->
            val isChanged = currentSource.applicationName == previousSource.applicationName &&
               ((currentSource.availableSources.hashCode() != previousSource.availableSources.hashCode()) || (currentSource.nameAndUrl != previousSource.nameAndUrl) )
            if (isChanged) {
               log().warn("currentSource: ${currentSource.applicationName}, ${currentSource.availableSources.hashCode()}, ${currentSource.nameAndUrl}")
               log().warn("previousSource: ${previousSource.applicationName}, ${previousSource.availableSources.hashCode()}, ${previousSource.nameAndUrl}")
            }
            isChanged
         }
      } + unhealthySources).distinct()

      if (unhealthySources.isNotEmpty()) {
         log().info("When calculating a schema delta, there are currently ${unhealthySources.size} source(s) that are unhealthy, and are being treated as changed: ${unhealthySources.joinToString { it.nameAndUrl }}")
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

   private fun concatUrlParts(ipAddr: String, port: Int, schemaUrlPath: String): String {
      return "http://$ipAddr:$port/${schemaUrlPath.removePrefix("/")}"
   }

   private fun verifyAllInstancesContainTheSameMappings(application: Application, publishedSources: List<Pair<InstanceInfo, SourcePublisherRegistration>>): SourcePublisherRegistration {
      val sourceRegistrationsBySourceHashes = publishedSources.associateBy { it.second.availableSources.hashCode() }
      if (sourceRegistrationsBySourceHashes.size > 1) {
         log().warn("Application ${application.name} has multiple instances that publish different schemas. Will use the first")
      }

      // Let's gather up the urls so that if one instance goes down, we can try the others
      val allUrls = publishedSources.flatMap { it.second.sourceUrls }
      val combinedRegistration = publishedSources.first().second.copy(sourceUrls = allUrls)
      return combinedRegistration
   }

   override fun schemaSet(): SchemaSet {
      return this.schemaStore.schemaSet()
   }

   override val generation: Int
      get() = this.schemaStore.generation

   override fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema> {
      refreshExecutorService.submit {
         schemaStore.submitSchemas(versionedSources)
      }
      return schemaStore.schemaSet().schema.right()
   }
}

