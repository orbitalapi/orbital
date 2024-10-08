package com.orbitalhq.schemaServer.core.schemaStoreConfig.clustered

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import com.hazelcast.map.listener.EntryUpdatedListener
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SchemaSetChangedEventRepository
import com.orbitalhq.schemaStore.SchemaSetCacheKey
import com.orbitalhq.schemaStore.ValidatingSchemaStoreClient
import mu.KotlinLogging

class DistributedSchemaStoreClient(hazelcast: HazelcastInstance):
   ValidatingSchemaStoreClient(
      schemaSetHolder = hazelcast.getMap("schemaSetHolderMap"),
      packagesById = hazelcast.getMap("schemaSourcesMap")) {

   init {

      (schemaSetHolder as IMap<SchemaSetCacheKey, SchemaSet>)
         .addEntryListener(SchemaHolderMapEventListener(this), true)
   }
   private val generationCounter = hazelcast.cpSubsystem.getAtomicLong("schemaGenerationCounter")
   override fun incrementGenerationCounterAndGet(): Int {
      return generationCounter.incrementAndGet().toInt()
   }

   override val generation: Int
      get() = generationCounter.get().toInt()
}

class SchemaHolderMapEventListener(private val schemaSetChangedEventRepository: SchemaSetChangedEventRepository): EntryUpdatedListener<SchemaSetCacheKey, SchemaSet> {
   private val logger = KotlinLogging.logger {}
   override fun entryUpdated(update: EntryEvent<SchemaSetCacheKey, SchemaSet>) {
      logger.info { "Distributed schemaSetHolderMap has an update: from ${update.member.uuid} - ${update.oldValue?.generation} / ${update.value?.generation}  is local member => ${update.member.localMember()}" }
      if (!update.member.localMember()) {
         // If there is an update on another Flow Node, trigger the schema notification so that UIs connected to
         // this node will get notifications about the schema update.
         schemaSetChangedEventRepository.emitNewSchemaIfDifferent(update.value)
      }
   }
}
