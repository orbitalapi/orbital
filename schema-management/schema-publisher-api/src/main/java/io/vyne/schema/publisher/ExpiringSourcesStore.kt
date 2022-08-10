package io.vyne.schema.publisher

import arrow.core.extensions.list.functorFilter.filter
import io.vyne.PackageIdentifier
import io.vyne.SourcePackage
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


private val logger = KotlinLogging.logger { }


/**
 * 1. Holds the schema submission made by individual schema publishers.
 * 2. Maintains the heartbeating state for each publisher.
 *
 * @param scheduler - Schedules state mutating operations onto the same thread to avoid race condition issues
 * in modifying mutable state maps.
 *
 *
 */
class ExpiringSourcesStore(
   private val keepAliveStrategyMonitors: List<KeepAliveStrategyMonitor>,
   // Sources is passable as an external val because our clustered setup requires it - in a cluster,
   // this map is managed by Hazelcast.
   internal val sources: ConcurrentMap<String, SourcePackage> = ConcurrentHashMap()
) {
   private val emitFailureHandler = Sinks.EmitFailureHandler { _: SignalType?, emitResult: Sinks.EmitResult ->
      (emitResult
         == Sinks.EmitResult.FAIL_NON_SERIALIZED)
   }
   private val sink = Sinks.many().multicast()
      .onBackpressureBuffer<PackagesUpdatedMessage>()

   val currentSources: Flux<PackagesUpdatedMessage> = sink.asFlux()

   init {
      keepAliveStrategyMonitors.forEach { keepAliveStrategyMonitor ->
         Flux.from(keepAliveStrategyMonitor.terminatedInstances).subscribe { zombieSource ->
            logger.info { "Received a zombie publisher detection => $zombieSource" }
            removeSources(zombieSource.publisherId)
         }
      }
   }

   fun removeSources(publisherId: String, emitUpdateMessage: Boolean = true): PackagesUpdatedMessage? {
      val updateMessage = sources.remove(publisherId)?.let { sourcePackage ->
         buildAndEmitUpdateMessage(listOf(sourcePackage.identifier), emitUpdateMessage)
      }
      return updateMessage
   }

   /**
    * Submits sources to for the provided id.
    *
    * If calling this operation results in a mutation, a separate updated state is emitted on the
    * current state flux.
    *
    * Returns the emitted SourcesUpdatedMessage.
    *
    * The caller may choose to supress the update message, in which case they are responsible
    * for handling downstream processing
    */
   fun submitSources(
      submission: KeepAlivePackageSubmission
   ): PackagesUpdatedMessage {
      // Previously, we were detecting for an updated publication where a file was removed.
      // However, I don't think that's needed now, as we're distributing packages, rather than
      // individual files.
      TODO()
//      val publisherId = submission.publisherId
//      val removedSchemaIds = when (val existingSubmission = sources[publisherId]) {
//         null -> {
//            sources[publisherId] = submission
//            emptyList()
//         }
//         else -> {
//            // T0: publisherA publishes - a.taxi, b.taxi
//            // T1: publisherA publishes - a.taxi
//            val removedVersionedSourceIds =
//               existingSubmission.sources.filter { existingVersionedSource ->
//                  submission.sources.none { it.name == existingVersionedSource.name }
//               }
//                  .map { it.id }
//            sources[publisherId] = submission
//            removedVersionedSourceIds
//         }
//      }
//      sources[submission.publisherId] = submission
////      notifyKeepAlive(submission)
//      return buildAndEmitUpdateMessage(emptyList(), emitUpdateMessage)
   }

   private fun notifyKeepAlive(submission: KeepAlivePackageSubmission) {
      keepAliveStrategyMonitors
         .filter { it.appliesTo(submission.keepAlive) }
         .forEach { it.monitor(submission.publisherConfig()) }
   }

   private fun buildSourcesUpdatesMessage(removedSchemaIds: List<PackageIdentifier>): PackagesUpdatedMessage {
      val currentSources = sources.values.toList()
      return PackagesUpdatedMessage(currentSources, listOf())
   }

   private fun buildAndEmitUpdateMessage(
      removedSchemaIds: List<PackageIdentifier>,
      emit: Boolean
   ): PackagesUpdatedMessage {
      val message = buildSourcesUpdatesMessage(removedSchemaIds)
      if (emit) {
         this.sink.emitNext(message, emitFailureHandler)
      }
      return message
   }
}

/**
 * The current set of sources as held by this SchemaSoreService
 */
data class PackagesUpdatedMessage(
   val currentPackages: List<SourcePackage>,
   val deltas: List<PackageDelta>
) {
   /**
    * Schema Ids that were removed since the last status message
    */
   val removedSchemaIds: List<PackageIdentifier> = deltas
      .filterIsInstance<PackageRemoved>()
      .map { it.oldStateId }
}

