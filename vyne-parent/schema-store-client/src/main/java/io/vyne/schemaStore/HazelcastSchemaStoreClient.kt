package io.vyne.schemaStore

import com.hazelcast.core.*
import com.hazelcast.map.EntryBackupProcessor
import com.hazelcast.map.EntryProcessor
import com.hazelcast.map.listener.EntryAddedListener
import com.hazelcast.map.listener.EntryUpdatedListener
import com.hazelcast.query.Predicate
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaSetChangedEvent
import lang.taxi.CompilationException
import lang.taxi.utils.log
import org.funktionale.either.Either
import org.springframework.context.ApplicationEventPublisher
import java.io.Serializable

private class HazelcastSchemaStoreListener(val eventPublisher:ApplicationEventPublisher, val invalidationListener: SchemaSetInvalidatedListener) : MembershipListener, Serializable, EntryAddedListener<SchemaSetCacheKey,SchemaSet>, EntryUpdatedListener<SchemaSetCacheKey,SchemaSet> {
   override fun memberAttributeChanged(memberAttributeEvent: MemberAttributeEvent?) {
   }

   override fun memberRemoved(event: MembershipEvent) {
      log().info("Cluster member removed, invalidating schema cache")
      invalidateCache()
   }

   override fun memberAdded(event: MembershipEvent) {
      log().info("Cluster member added, invalidating schema cache")
      invalidateCache()
   }

   fun invalidateCache() {
      log().info("Cache changing")
//      val oldSchemaSet = schemaCache[SchemaSetCacheKey]
//      schemaCache.remove(SchemaSetCacheKey)
      invalidationListener.rebuildRequired()
   }

   override fun entryAdded(event: EntryEvent<SchemaSetCacheKey, SchemaSet>) {
      log().info("SchemaSet has been created: ${event.value.toString()} - dispatching event.")
      eventPublisher.publishEvent(SchemaSetChangedEvent(event.oldValue, event.value))
   }

   override fun entryUpdated(event: EntryEvent<SchemaSetCacheKey, SchemaSet>) {
      log().info("SchemaSet has changed: ${event.value.toString()} - dispatching event")
      eventPublisher.publishEvent(SchemaSetChangedEvent(event.oldValue, event.value))

   }
}

interface SchemaSetInvalidatedListener {
   fun rebuildRequired()
}


internal object SchemaSetCacheKey : Serializable
class HazelcastSchemaStoreClient(private val hazelcast: HazelcastInstance, private val schemaValidator: SchemaValidator = TaxiSchemaValidator(), val eventPublisher: ApplicationEventPublisher) : SchemaStoreClient, SchemaSetInvalidatedListener {

   private val generationCounter = hazelcast.getAtomicLong("schemaGenerationIndex")
   private val schemaSetHolder: IMap<SchemaSetCacheKey, SchemaSet> = hazelcast.getMap("schemaSet")
   private val schemaSourcesMap: IMap<SchemaId, CacheMemberSchema> = hazelcast.getMap("vyneSchemas")
   private val hazelcastSchemaStoreListener = HazelcastSchemaStoreListener(eventPublisher, this)
   private val schemaPurger = HazelcastSchemaPurger(schemaSourcesMap)

   init {
      hazelcast.cluster.addMembershipListener(hazelcastSchemaStoreListener)

      // We add a listener on the schemaSet, as we want to broadcast events when the schemaSet changes.
      // The node running this code may not have been the node that triggered the change, so we have to work
      // in an observer, rather than in the change / invalidation code.
      schemaSetHolder.addEntryListener(hazelcastSchemaStoreListener, true)
   }

   override val generation: Int
      get() {
         return generationCounter.get().toInt()
      }


   override fun submitSchema(schemaName: String, schemaVersion: String, schema: String): Either<CompilationException, Schema> {
      val versionedSchema = VersionedSchema(schemaName, schemaVersion, schema)
      // TODO : This creates a race condition where multiple schemas can pass validation at the same time
      val validationResult = schemaValidator.validate(schemaSet(), versionedSchema)
      validationResult.right().map { validatedSchema ->

         // TODO : Here, we're still storing ONLY the raw schema we've received, not the merged schema.
         // That seems wasteful, as we're just gonna re-compute this later.
         val cachedSchema = CacheMemberSchema(hazelcast.cluster.localMember.uuid, versionedSchema)
         schemaSourcesMap[versionedSchema.id] = cachedSchema
         rebuildSchemaAndWriteToCache()
      }
      validationResult.left().map { compilationException ->
         log().error("Schema ${versionedSchema.id} is rejected for compilation exception: ${compilationException.message}")
      }
      return validationResult
   }

   private fun rebuildSchemaAndWriteToCache(): SchemaSet {
      // Note:  I'm worried about a potential race condition here,
      // as this code executes on all the nodes who are participating in the distributed
      // schema cluster.
      // We should consider moving this to a distributed work queue, so the schemaSet is only rebuilt
      // once across the cluser.
      val currentClusterMembers = hazelcast.cluster.members.map { it.uuid }
      schemaPurger.removeOldSchemasFromHazelcast(currentClusterMembers)

      val sources = getSchemaEntriesOfCurrentClusterMembers()
      val result = SchemaSet(sources, generationCounter.incrementAndGet().toInt())
      log().info("Rebuilt schema cache - $result")
      schemaSetHolder.compute(SchemaSetCacheKey) { _, current ->
         when {
            current == null -> {
               log().info("Persisting first schema to cache: $result")
               result
            }
            current.generation >= result.generation -> {
               log().info("Not updating the cache for $result, as the current seems later. (Current: $current)")
               current
            }
            else -> {
               log().info("Updating schema cache with $result")
               result
            }
         }
      }
      return result
   }


   override fun schemaSet(): SchemaSet {
      return schemaSetHolder.computeIfAbsent(SchemaSetCacheKey) {
         // Note: This should very rarely get called,
         // as we're actively rebuilding the schemaSet on invalidation now (whereas previously
         // we deferred that)
         log().warn("SchemaSet was not present, so computing, however this shouldn't happen")
         rebuildSchemaAndWriteToCache()
      }
   }

   private fun getSchemaEntriesOfCurrentClusterMembers(): List<VersionedSchema> {
      return schemaSourcesMap.filter { (_, cacheMemberSchema) ->
         hazelcast.cluster.members.any { it.uuid == cacheMemberSchema.cacheMemberId }
      }.map { (_, value) -> value.schema }
   }

   override fun rebuildRequired() {
      log().info("Rebuild of Schema triggered through cache invalidation")
      val schemaSet = rebuildSchemaAndWriteToCache()
      schemaSetHolder.submitToKey(SchemaSetCacheKey,RebuildSchemaSetTask(schemaSet))
   }
}

private class RebuildSchemaSetTask(private val schemaSet: SchemaSet) : EntryProcessor<SchemaSetCacheKey,SchemaSet>, EntryBackupProcessor<SchemaSetCacheKey,SchemaSet> {
   override fun getBackupProcessor(): EntryBackupProcessor<SchemaSetCacheKey, SchemaSet>? {
      return this;
   }

   override fun process(entry: MutableMap.MutableEntry<SchemaSetCacheKey, SchemaSet>): Any {
      log().info("Updating schema in cache to generation ${schemaSet.generation} with ${schemaSet.sources.size} sources")
      entry.setValue(schemaSet)
      return schemaSet
   }

   override fun processBackup(entry: MutableMap.MutableEntry<SchemaSetCacheKey, SchemaSet>) {
      log().info("Updating schema in backup cache to generation ${schemaSet.generation} with ${schemaSet.sources.size} sources")
      entry.setValue(schemaSet)
   }

}

class HazelcastSchemaPurger(private val hazelcastMap: IMap<SchemaId, CacheMemberSchema>) : Serializable {

   fun removeOldSchemasFromHazelcast(currentClusterMembers: List<String>) {
      hazelcastMap.removeAll(object : Predicate<SchemaId, CacheMemberSchema> {
         override fun apply(mapEntry: MutableMap.MutableEntry<SchemaId, CacheMemberSchema>): Boolean {
            return if (currentClusterMembers.none { it == mapEntry.value.cacheMemberId }) {
               log().info("Cluster member for schema ${mapEntry.key} has gone away, and it's schema is being removed")
               true
            } else {
               false
            }
         }
      })
   }
}

data class CacheMemberSchema(val cacheMemberId: String, val schema: VersionedSchema) : Serializable
