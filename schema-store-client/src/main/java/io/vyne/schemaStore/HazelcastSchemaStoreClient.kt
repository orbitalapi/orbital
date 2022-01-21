package io.vyne.schemaStore

import arrow.core.Either
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.hazelcast.cluster.MembershipEvent
import com.hazelcast.cluster.MembershipListener
import com.hazelcast.core.EntryEvent
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.EntryProcessor
import com.hazelcast.map.IMap
import com.hazelcast.map.listener.EntryAddedListener
import com.hazelcast.map.listener.EntryUpdatedListener
import com.hazelcast.query.Predicate
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemaApi.SchemaSet
import io.vyne.schemaApi.SchemaValidator
import io.vyne.schemaConsumerApi.SchemaChangedEventProvider
import io.vyne.schemaConsumerApi.SchemaSetChangedEventRepository
import io.vyne.schemaConsumerApi.SchemaStore
import io.vyne.schemaPublisherApi.SchemaPublisher
import io.vyne.schemaPublisherApi.VersionedSourceSubmission
import io.vyne.schemas.DistributedSchemaConfig
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import lang.taxi.utils.log
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import java.io.Serializable
import java.util.concurrent.Executors
import java.util.function.BiFunction
import kotlin.concurrent.thread

private class HazelcastSchemaStoreListener(
   val eventPublisher: ApplicationEventPublisher,
   val invalidationListener: SchemaSetInvalidatedListener) :
   MembershipListener, Serializable, EntryAddedListener<SchemaSetCacheKey, SchemaSet>, EntryUpdatedListener<SchemaSetCacheKey, SchemaSet> {
   /**
    * Used to dispatch Hazelcast map / cluster events to the actual consumer as these events are arrived in hazelcast's event dispatcher threads
    * which should not be blocked with expensive operations.
    */
   private val eventDispatcher = Executors
      .newFixedThreadPool(1, ThreadFactoryBuilder().setNameFormat("DistributedSchemaEventDispatcher-%d").build())
   override fun memberRemoved(event: MembershipEvent) {
      log().info("Cluster ${event.member} removed, invalidating schema cache")
      eventDispatcher.submit { invalidationListener.rebuildRequired() }
   }

   override fun memberAdded(event: MembershipEvent) {
      log().info("Cluster ${event.member} added.")
   }

   override fun entryAdded(event: EntryEvent<SchemaSetCacheKey, SchemaSet>) {
      log().info("SchemaSet on generation ${event.value.generation} Added")
     notifySchemaSetChange(event)
   }

   override fun entryUpdated(event: EntryEvent<SchemaSetCacheKey, SchemaSet>) {
      log().info("SchemaSet updated from generation ${event.oldValue?.generation} to ${event.value.generation}")
      notifySchemaSetChange(event)
   }

   private fun notifySchemaSetChange(event: EntryEvent<SchemaSetCacheKey, SchemaSet>) {
      invalidationListener.updateCurrentSchemaSet(event.value)
      eventDispatcher.submit {
         invalidationListener.onSchemaSetChanged(event.oldValue, event.value)
      }
   }
}

interface SchemaSetInvalidatedListener: SchemaChangedEventProvider {
   fun rebuildRequired()
   fun updateCurrentSchemaSet(newSchemaSet: SchemaSet)
   fun onSchemaSetChanged(oldSchemaSet: SchemaSet?, newSchemaSet: SchemaSet)
}

private val logger = KotlinLogging.logger {}

object SchemaSetCacheKey : Serializable
@Deprecated("DISTRIBUTED Schema Distribution mode replaced with RSOCKET / HTTP based schema consumption / publication mechanisms")
class HazelcastSchemaStoreClient(
   private val hazelcast: HazelcastInstance,
   private val schemaValidator: SchemaValidator = TaxiSchemaValidator(),
   private val eventPublisher: ApplicationEventPublisher
) : SchemaSetChangedEventRepository(), SchemaStore, SchemaPublisher, SchemaSetInvalidatedListener {
   private val generationCounter = hazelcast.cpSubsystem.getAtomicLong("schemaGenerationIndex")

   /**
    * schemaSetHolder is configured as NearCache to speed up the reads, see VyneHazelcastConfig::vyneHazelcastInstance()
    */
   private val schemaSetHolder: IMap<SchemaSetCacheKey, SchemaSet> = hazelcast.getMap(DistributedSchemaConfig.SchemaCacheName)
   private val schemaSourcesMap: IMap<SchemaId, CacheMemberSchema> = hazelcast.getMap("vyneSchemas")
   private val hazelcastSchemaStoreListener = HazelcastSchemaStoreListener(eventPublisher, this)
   private val schemaPurger = HazelcastSchemaPurger(schemaSourcesMap)
   private val rebuildTaskQueue = hazelcast.getQueue<Int>("rebuildTaskQueue")
   @Volatile
   private var currentSchemaSet: SchemaSet? = null

   init {
      hazelcast.cluster.addMembershipListener(hazelcastSchemaStoreListener)

      // We add a listener on the schemaSet, as we want to broadcast events when the schemaSet changes.
      // The node running this code may not have been the node that triggered the change, so we have to work
      // in an observer, rather than in the change / invalidation code.
      schemaSetHolder.addEntryListener(hazelcastSchemaStoreListener, true)
      thread(start = true) {
         try {
            while (true) {
               val generation = rebuildTaskQueue.take()
               log().info("rebuilding schema for trigger $generation")
               val schemaSet = rebuildSchemaAndWriteToCache()
               schemaSetHolder.submitToKey(SchemaSetCacheKey, RebuildSchemaSetTask(schemaSet.first))
            }
         } catch (e: Exception) {
            log().error("Error in processing schema rebuild", e)
         }
      }


   }


   override val generation: Int
      get() {
         return generationCounter.get().toInt()
      }


   override fun submitSchemaPackage(
      sourcePackage: VersionedSourceSubmission,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      // Mimic legacy behaviour for Hazelcast
      return submitSchemas(sourcePackage.sources, removedSources)
   }
   override fun submitSchemas(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      logger.info { "Submitting the following schemas: ${versionedSources.joinToString { it.id }}" }
      logger.info { "Removing the following schemas: ${removedSources.joinToString { it }}" }
      versionedSources.forEach { versionedSource ->
         val cachedSource = CacheMemberSchema(hazelcast.cluster.localMember.uuid.toString(), versionedSource)
         log().info("Member=${hazelcast.cluster.localMember.uuid} added new schema ${versionedSource.id} to it's cache")
         schemaSourcesMap[versionedSource.id] = cachedSource
      }

      val removedSchemaIds = if (removedSources.isNotEmpty()) {
         val schemaNamesToBeRemoved = removedSources.map { VersionedSource.nameAndVersionFromId(it).first }.toSet()
         val removePredicate = SchemaRemovePredicate(schemaNamesToBeRemoved)
         val filteredSchemaIds = removePredicate.schemaIdsToBeRemoved(this.schemaSourcesMap.keys)
         schemaSourcesMap.removeAll(removePredicate)
         filteredSchemaIds
      } else emptyList()
      return rebuildSchemaAndWriteToCache(removedSchemaIds).second
   }

   private fun rebuildSchemaAndWriteToCache(removedSourceIds: List<SchemaId> = emptyList()): Pair<SchemaSet, Either<CompilationException, Schema>> {
      // Note:  I'm worried about a potential race condition here,
      // as this code executes on all the nodes who are participating in the distributed
      // schema cluster.
      // We should consider moving this to a distributed work queue, so the schemaSet is only rebuilt
      // once across the cluster.
      val currentClusterMembers = hazelcast.cluster.members.map { it.uuid.toString() }
      val removedSources = schemaPurger.removedEntries(currentClusterMembers)
      schemaPurger.removeOldSchemasFromHazelcast(currentClusterMembers)
      val sources = getSchemaEntriesOfCurrentClusterMembers()
      val currentSchemaSet = schemaSet()
      log().info("Current Schema Ids: ${currentSchemaSet.allSources.map { it.id }}, New Schema Ids: ${sources.map { it.id }}")
      val (parsedSources, returnValue) = schemaValidator.validateAndParse(currentSchemaSet, sources, removedSources + removedSourceIds)
      val result = SchemaSet.fromParsed(parsedSources, generationCounter.incrementAndGet().toInt())
      log().info("Rebuilt schema cache - $result")
      schemaSetHolder.compute(SchemaSetCacheKey, SchemaSetMutator(result))
      return Pair(result, returnValue.mapLeft { CompilationException(it) })
   }


   internal class SchemaSetMutator(private val result: SchemaSet) : BiFunction<SchemaSetCacheKey, SchemaSet?, SchemaSet?>, Serializable {
      override fun apply(key: SchemaSetCacheKey, current: SchemaSet?): SchemaSet? {
        return  when {
            current == null -> {
               result
            }
            current.generation >= result.generation -> {
               current
            }
            else -> {
               result
            }
         }
      }
   }


   override fun schemaSet(): SchemaSet {
      return currentSchemaSet ?: schemaSetHolder[SchemaSetCacheKey] ?: SchemaSet.EMPTY
   }

   private fun getSchemaEntriesOfCurrentClusterMembers(): List<VersionedSource> {
      return schemaSourcesMap
         .toMap()
         .filter { (_, cacheMemberSchema) ->
         hazelcast.cluster.members.any { it.uuid.toString() == cacheMemberSchema.cacheMemberId }
      }.map { (_, value) -> value.schema }
   }

   override fun rebuildRequired() {
      log().info("Rebuild of Schema triggered through cache invalidation")
      rebuildTaskQueue.put(this.generation)
   }

   override fun updateCurrentSchemaSet(newSchemaSet: SchemaSet) {
      currentSchemaSet = newSchemaSet
   }

   override fun onSchemaSetChanged(oldSchemaSet: SchemaSet?, newSchemaSet: SchemaSet) {
      publishSchemaSetChangedEvent(oldSchemaSet, newSchemaSet)
   }

}

private class RebuildSchemaSetTask(private val schemaSet: SchemaSet) : EntryProcessor<SchemaSetCacheKey, SchemaSet, SchemaSet> {
   override fun process(entry: MutableMap.MutableEntry<SchemaSetCacheKey, SchemaSet>): SchemaSet {
      log().info("Updating schema in cache to generation ${schemaSet.generation} with ${schemaSet.sources.size} sources")
      entry.setValue(schemaSet)
      return schemaSet
   }
}

class HazelcastSchemaPurger(private val hazelcastMap: IMap<SchemaId, CacheMemberSchema>) : Serializable {

   fun removedEntries(currentClusterMembers: List<String>): List<SchemaId> {
      return hazelcastMap.values.filter {
         currentClusterMembers.none { currentClusterMember -> currentClusterMember == it.cacheMemberId }
      }.map { it.schema.id }
   }
   fun removeOldSchemasFromHazelcast(currentClusterMembers: List<String>) {
      hazelcastMap.removeAll(object : Predicate<SchemaId, CacheMemberSchema> {
         override fun apply(mapEntry: MutableMap.MutableEntry<SchemaId, CacheMemberSchema>): Boolean {
            return if (currentClusterMembers.none { it == mapEntry.value.cacheMemberId }) {
               log().info("Member=${mapEntry.value.cacheMemberId} disconnected, removing schema=${mapEntry.key} from cache")
               true
            } else {
               false
            }
         }
      })
   }
}

data class CacheMemberSchema(val cacheMemberId: String, val schema: VersionedSource) : Serializable
class SchemaRemovePredicate(private val schemaNamesToBeRemoved: Set<String>) : Predicate<SchemaId, CacheMemberSchema> {
   fun schemaIdsToBeRemoved(schemaIds: Collection<SchemaId>): List<SchemaId> {
      return schemaIds.filter { schemaId ->
         val (name, _) = VersionedSource.nameAndVersionFromId(schemaId)
         schemaNamesToBeRemoved.contains(name)
      }
   }
   override fun apply(entry: MutableMap.MutableEntry<SchemaId, CacheMemberSchema>): Boolean {
      val (name, _) = VersionedSource.nameAndVersionFromId(entry.key)
      val shouldRemove = schemaNamesToBeRemoved.contains(name)
      if (shouldRemove) {
         log().info("removing source ${entry.key}")
      }
      return shouldRemove
   }
}
