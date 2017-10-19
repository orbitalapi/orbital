package io.polymer.schemaStore

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.hazelcast.core.*
import lang.taxi.utils.log

internal typealias ClusterMemberId = String

class HazelcastSchemaStoreClient(private val hazelcast:HazelcastInstance) : SchemaStoreClient, MembershipListener {
   private enum class SchemaSetCacheKey { INSTANCE }
   private val schemaSet:LoadingCache<SchemaSetCacheKey,SchemaSet> = CacheBuilder
      .newBuilder()
      .build(
      object : CacheLoader<SchemaSetCacheKey,SchemaSet>() {
         override fun load(p0: SchemaSetCacheKey?): SchemaSet {
            return SchemaSet(map().values.toList())
         }

      }
   )
   override fun memberRemoved(event: MembershipEvent) {
      val removedSchema = map().remove(event.member.uuid)
      if (removedSchema != null) {
         log().info("Removed schema ${removedSchema.id} because the owning member was removed from the cluster")
      } else {
         log().debug("Member ${event.member.uuid} has gone offline, but it's schema has already been removed, likely by another member.  No action required")
      }
      invalidateCache()
   }

   override fun memberAdded(event: MembershipEvent) {
      invalidateCache()
   }

   private fun invalidateCache() {
      log().info("Cluster membership changed, so invalidating the schema cache.  Will rebuild on next query")
      schemaSet.invalidateAll()
   }

   override fun memberAttributeChanged(p0: MemberAttributeEvent) {
   }

   init {
      hazelcast.cluster.addMembershipListener(this)
   }

   private fun map(): IMap<ClusterMemberId, VersionedSchema> {
      return hazelcast.getMap("vyneSchemas")
   }
   override fun submitSchema(schemaName: String, schemaVersion: String, schema: String) {
      map().put(hazelcast.cluster.localMember.uuid, VersionedSchema(schemaName,schemaVersion,schema))
   }

   override fun schemaSet(): SchemaSet {
      return schemaSet.get(SchemaSetCacheKey.INSTANCE)
   }

}
