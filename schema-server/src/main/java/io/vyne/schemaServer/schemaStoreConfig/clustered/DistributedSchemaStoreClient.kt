package io.vyne.schemaServer.schemaStoreConfig.clustered

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import com.hazelcast.map.listener.EntryUpdatedListener
import io.vyne.schema.api.SchemaSet
import io.vyne.schemaStore.SchemaSetCacheKey
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import mu.KotlinLogging

class DistributedSchemaStoreClient(hazelcast: HazelcastInstance):
   ValidatingSchemaStoreClient(
      schemaSetHolder = hazelcast.getMap("schemaSetHolderMap"),
      packagesById = hazelcast.getMap("schemaSourcesMap")) {

   init {
      (schemaSetHolder as IMap<SchemaSetCacheKey, SchemaSet>)
         .addEntryListener(SchemaHolderMapEventListener(), true)
   }
   private val generationCounter = hazelcast.cpSubsystem.getAtomicLong("schemaGenerationCounter")
   override fun incrementGenerationCounterAndGet(): Int {
      return generationCounter.incrementAndGet().toInt()
   }

   override val generation: Int
      get() = generationCounter.get().toInt()
}

class SchemaHolderMapEventListener: EntryUpdatedListener<SchemaSetCacheKey, SchemaSet> {
   private val logger = KotlinLogging.logger {}
   override fun entryUpdated(update: EntryEvent<SchemaSetCacheKey, SchemaSet>) {
      logger.info { "Distributed schemaSetHolderMap has an update: from ${update.member.uuid} - ${update.oldValue?.generation} / ${update.value?.generation}" }
   }
}
