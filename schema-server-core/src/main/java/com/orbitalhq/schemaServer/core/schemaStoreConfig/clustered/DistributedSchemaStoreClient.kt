package com.orbitalhq.schemaServer.core.schemaStoreConfig.clustered

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import com.hazelcast.map.listener.EntryAddedListener
import com.hazelcast.map.listener.EntryRemovedListener
import com.hazelcast.map.listener.EntryUpdatedListener
import com.orbitalhq.ParsedPackage
import com.orbitalhq.schema.publisher.ExpiringSourcesStore
import com.orbitalhq.schema.publisher.KeepAlivePackageSubmission
import com.orbitalhq.schema.publisher.PackageRemoved
import com.orbitalhq.schema.publisher.PackagesUpdatedMessage
import com.orbitalhq.schemaStore.ValidatingSchemaStoreClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DistributedSchemaStoreClient(hazelcast: HazelcastInstance,
                                   packagesByIdMap: IMap<String, ParsedPackage>
):
   ValidatingSchemaStoreClient(
      /**
       * Here we use an in memory concurrent cache, rather than an IMap
       * The reason for that is that the SchemaSet is not fully Serializable
       * We bypass this by making _taxiSchemas: List<TaxiSchema>? member Transient, but
       * this cause another issue of initialising When before we update the SchemaSet instance in schemaSetHolder
       * This initialisation has not effect when we use an IMap as _taxiSchemas is Transient, so we end up with calling
       * expensive initialisation method whenever we require a SchemaSet.
       */
      schemaSetHolder = ConcurrentHashMap(),
      packagesById = packagesByIdMap) {

   private val generationCounter = hazelcast.getPNCounter("schemaGenerationCounter")
   override fun incrementGenerationCounterAndGet(): Int {
      return generationCounter.incrementAndGet().toInt()
   }

   override val generation: Int
      get() = generationCounter.get().toInt()

   companion object {
      const val SCHEMA_SOURCES_MAP = "schemaSourcesMap"
   }
}

/**
 * Can't use UnversionedPackageIdentifier here as String alias due to
 * https://youtrack.jetbrains.com/issue/KT-24700
 */
class ParsedPackageMapEventListener(private val expiringSourceStore: ExpiringSourcesStore,
                                    private val validatingStore: ValidatingSchemaStoreClient,
                                    private val currentMemberId: UUID? = null):
   EntryUpdatedListener<String, ParsedPackage>,
   EntryAddedListener<String, ParsedPackage>,
   EntryRemovedListener<String, ParsedPackage> {
   private val logger = KotlinLogging.logger {}
   override fun entryUpdated(update: EntryEvent<String, ParsedPackage>) {
      processAddOrUpdated(update)
   }

   override fun entryAdded(add: EntryEvent<String, ParsedPackage>) {
      processAddOrUpdated(add)
   }

   private fun processAddOrUpdated(event: EntryEvent<String, ParsedPackage>) {
      val oldHash = event.oldValue?.sources?.map { it.source.contentHash }?.sorted()?.toSet() ?: emptySet()
      val newHash = event.value?.sources?.map { it.source.contentHash }?.sorted()?.toSet() ?: emptySet()
      val isLocalUpdate = currentMemberId?.let { event.member.uuid == it } ?: event.member.localMember()
      logger.debug { "Parsed Package has an ${event.eventType}: from ${event.member.uuid} - ${event.oldValue?.metadata?.submissionDate} / ${event.value?.metadata?.submissionDate}  " +
              "local => ${event.member.localMember()}, oldHash => $oldHash newHash => $newHash" }
      if (oldHash != newHash && !isLocalUpdate) {
         logger.debug { "Injecting package updated event from ${event.member.uuid}" }

         val packageUpdateMessage =   expiringSourceStore.submitSources(KeepAlivePackageSubmission(
            sourcePackage = event.value.toSourcePackage(),
            publisherId = event.member.uuid.toString()))
         validatingStore.submitUpdates(packageUpdateMessage)
      }

   }


   override fun entryRemoved(remove: EntryEvent<String, ParsedPackage>) {
      val isLocalUpdate = currentMemberId?.let { remove.member.uuid == it } ?: remove.member.localMember()
      logger.debug { "Parsed Package has a Remove: from ${remove.member.uuid} - ${remove.oldValue?.metadata?.submissionDate} / ${remove.value?.metadata?.submissionDate} local => $isLocalUpdate }"}
      if (!isLocalUpdate) {
         logger.debug { "Injecting package removed event from ${remove.member.uuid}" }
         validatingStore.submitUpdates(PackagesUpdatedMessage(emptyList(), listOf(PackageRemoved(remove.value.toSourcePackage()))))
      }
   }
}

@ConditionalOnExpression("\${vyne.schema.server.clustered:false}")
@Component
class ParsedPackageMapEventListenerConfigurer(expiringSourceStore: ExpiringSourcesStore,
                                              validatingStore: ValidatingSchemaStoreClient,
                                              @Qualifier(DistributedSchemaStoreClient.SCHEMA_SOURCES_MAP) packagesByIdMap: IMap<String, ParsedPackage>,
                                              currentMemberId: UUID? = null
) {

   private val parsedPackageMapEventListener = ParsedPackageMapEventListener(expiringSourceStore, validatingStore, currentMemberId)
   init {
      packagesByIdMap.addEntryListener(parsedPackageMapEventListener, true)
   }
}

