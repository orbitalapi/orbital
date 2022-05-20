package io.vyne.schema.publisher

import arrow.core.extensions.list.functorFilter.filter
import io.vyne.SchemaId
import io.vyne.VersionedSource
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
   internal val sources: ConcurrentMap<String, VersionedSourceSubmission> = ConcurrentHashMap()
) {
   private val emitFailureHandler = Sinks.EmitFailureHandler { _: SignalType?, emitResult: Sinks.EmitResult ->
      (emitResult
         == Sinks.EmitResult.FAIL_NON_SERIALIZED)
   }
   private val sink = Sinks.many().multicast()
      .onBackpressureBuffer<SourcesUpdatedMessage>()

   val currentSources: Flux<SourcesUpdatedMessage> = sink.asFlux()

   init {
      keepAliveStrategyMonitors.forEach { keepAliveStrategyMonitor ->
         Flux.from(keepAliveStrategyMonitor.terminatedInstances).subscribe { zombieSource ->
            logger.info { "Received a zombie publisher detection => $zombieSource" }
            removeSources(zombieSource.publisherId)
         }
      }
   }

   fun removeSources(publisherId: String, emitUpdateMessage: Boolean = true): SourcesUpdatedMessage? {
      val updateMessage = sources.remove(publisherId)?.let { submission ->
         val removedVersionSourceIds = submission.sources.map { versionedSource -> versionedSource.id }
         buildAndEmitUpdateMessage(removedVersionSourceIds, emitUpdateMessage)
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
      submission: VersionedSourceSubmission,
      emitUpdateMessage: Boolean = true
   ): SourcesUpdatedMessage {
      val publisherId = submission.publisherId
      val removedSchemaIds = when (val existingSubmission = sources[publisherId]) {
         null -> {
            sources[publisherId] = submission
            emptyList()
         }
         else -> {
            // T0: publisherA publishes - a.taxi, b.taxi
            // T1: publisherA publishes - a.taxi
            val removedVersionedSourceIds =
               existingSubmission.sources.filter { existingVersionedSource ->
                  submission.sources.none { it.name == existingVersionedSource.name }
               }
                  .map { it.id }
            sources[publisherId] = submission
            removedVersionedSourceIds
         }
      }
      sources[submission.publisherId] = submission
      notifyKeepAlive(submission)
      return buildAndEmitUpdateMessage(removedSchemaIds, emitUpdateMessage)
   }

   private fun notifyKeepAlive(submission: VersionedSourceSubmission) {
      keepAliveStrategyMonitors
         .filter { it.appliesTo(submission.keepAlive) }
         .forEach { it.monitor(submission.publisherConfig()) }
   }

   private fun buildSourcesUpdatesMessage(removedSchemaIds: List<SchemaId>): SourcesUpdatedMessage {
      val currentSources = this.sources.values.flatMap { it.sources }
      return SourcesUpdatedMessage(currentSources, removedSchemaIds)
   }

   private fun buildAndEmitUpdateMessage(
      removedSchemaIds: List<SchemaId>,
      emit: Boolean
   ): SourcesUpdatedMessage {
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
data class SourcesUpdatedMessage(
   val sources: List<VersionedSource>,
   /**
    * Schema Ids that were removed since the last status message
    */
   val removedSchemaIds: List<SchemaId>,
)

