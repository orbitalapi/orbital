package io.polymer.schemaStore

import com.diffplug.common.base.Either
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.hazelcast.core.*
import io.osmosis.polymer.CompositeSchemaBuilder
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.utils.log
import reactor.core.publisher.Mono

internal typealias ClusterMemberId = String

class HazelcastSchemaStoreClient(private val hazelcast: HazelcastInstance, private val schemaValidator:SchemaValidator) : SchemaStoreClient, MembershipListener {
   private enum class SchemaSetCacheKey { INSTANCE }

   private val schemaSet: LoadingCache<SchemaSetCacheKey, SchemaSet> = CacheBuilder
      .newBuilder()
      .build(
         object : CacheLoader<SchemaSetCacheKey, SchemaSet>() {
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

   override fun submitSchema(schemaName: String, schemaVersion: String, schema: String): Mono<SchemaSetId> {
      val versionedSchema = VersionedSchema(schemaName, schemaVersion, schema)
      return Mono.create { sink ->
         // TODO : This creates a race condition where multiple schemas can pass validation at the same time
         schemaValidator.validate(schemaSet(),versionedSchema)
         map().put(hazelcast.cluster.localMember.uuid, VersionedSchema(schemaName, schemaVersion, schema))
      }

   }

   private fun validate(versionedSchema: VersionedSchema): Mono<CompilationError> {
      val schemas = schemaSet().add(versionedSchema)
      CompositeSchemaBuilder().aggregate(schemas.schemas)
      // TODO
      return null
   }

   override fun schemaSet(): SchemaSet {
      return schemaSet.get(SchemaSetCacheKey.INSTANCE)
   }

}
