package io.vyne.schemaStore

import com.hazelcast.core.*
import com.hazelcast.query.Predicate
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import lang.taxi.utils.log
import org.funktionale.either.Either
import reactor.core.publisher.Mono
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal typealias ClusterMemberId = String

private class HazelcastSchemaStoreListener(val schemaCache: ConcurrentHashMap<SchemaSetCacheKey, SchemaSet>) : MembershipListener, Serializable {
   override fun memberAttributeChanged(memberAttributeEvent: MemberAttributeEvent?) {
   }

   override fun memberRemoved(event: MembershipEvent) {
      invalidateCache()
   }

   override fun memberAdded(event: MembershipEvent) {
      invalidateCache()
   }

   fun invalidateCache() {
      log().info("Cluster membership changed, so invalidating the schema cache.  Will rebuild on next queryEngine")
      schemaCache.remove(SchemaSetCacheKey)
   }

}

private object SchemaSetCacheKey : Serializable
class HazelcastSchemaStoreClient(private val hazelcast: HazelcastInstance, private val schemaValidator: SchemaValidator = TaxiSchemaValidator()) : SchemaStoreClient {


   private val generationCounter = AtomicInteger(0)
   private val schemaSetHolder = ConcurrentHashMap<SchemaSetCacheKey, SchemaSet>()
   private val hazelcastMap: IMap<SchemaId, CacheMemberSchema> = hazelcast.getMap("vyneSchemas")
   private val hazelcastSchemaStoreListener = HazelcastSchemaStoreListener(schemaSetHolder)
   private val schemaPurger = HazelcastSchemaPurger(hazelcastMap)

   init {

      hazelcast.cluster.addMembershipListener(hazelcastSchemaStoreListener)
   }

   override val generation: Int
      get() {
         return generationCounter.get()
      }


//
//      val removedSchema = map().remove(event.member.uuid)
//      if (removedSchema != null) {
//         log().info("Removed schema ${removedSchema.id} because the owning member was removed from the cluster")
//      } else {
//         log().debug("Member ${event.member.uuid} has gone offline, but it's schema has already been removed, likely by another member.  No action required")
//      }


   override fun submitSchema(schemaName: String, schemaVersion: String, schema: String): Mono<Either<CompilationException, Schema>> {
      val versionedSchema = VersionedSchema(schemaName, schemaVersion, schema)
      return Mono.create { sink ->
         // TODO : This creates a race condition where multiple schemas can pass validation at the same time
         val validationResult = schemaValidator.validate(schemaSet(), versionedSchema)
         validationResult.right().map { validatedSchema ->

            // TODO : Here, we're still storing ONLY the raw schema we've received, not the merged schema.
            // That seems wasteful, as we're just gonna re-compute this later.

            val cachedSchema = CacheMemberSchema(hazelcast.cluster.localMember.uuid, versionedSchema)
            hazelcastMap[versionedSchema.id] = cachedSchema
            hazelcastSchemaStoreListener.invalidateCache()
//            sink.success(schemaSet().id)
         }
         validationResult.left().map { compilationException ->
            log().error("Schema ${versionedSchema.id} is rejected for compilation exception: ${compilationException.message}")
//            sink.error(compilationException)
         }
         // TODO : This feels incorrect, calling success with an either which may model failure.
         sink.success(validationResult)
      }

   }


   override fun schemaSet(): SchemaSet {
      return schemaSetHolder.computeIfAbsent(SchemaSetCacheKey) {
         val currentClusterMembers = hazelcast.cluster.members.map { it.uuid }
         schemaPurger.removeOldSchemasFromHazelcast(currentClusterMembers)

         val sources = hazelcastMap.filter { (_, cacheMemberSchema) ->
            hazelcast.cluster.members.any { it.uuid == cacheMemberSchema.cacheMemberId }
         }.map { (_, cacheSchemaMember) -> cacheSchemaMember.schema }
         val result = SchemaSet(sources, generationCounter.incrementAndGet())
         log().info("Rebuilt schema cache, now on generation $generation, with id ${result.id}, containing ${result.size()} schemas")
         result
      }
   }


}

class HazelcastSchemaPurger(private val hazelcastMap: IMap<SchemaId, CacheMemberSchema>) : Serializable {

   fun removeOldSchemasFromHazelcast(currentClusterMembers: List<String>) {
      hazelcastMap.removeAll(object:Predicate<SchemaId,CacheMemberSchema> {
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
