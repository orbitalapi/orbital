package io.vyne.schemaPublisherApi

import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemaApi.SchemaSet
import lang.taxi.CompilationError
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
            sources.remove(zombieSource.publisherId)?.let { submission ->
               val removedVersionSourceIds = submission.sources.map {  versionedSource -> versionedSource.id }
               emitRemovedSchemaIds(removedVersionSourceIds)
            }
         }
      }
   }

   /**
    * Submits sources to for the provided id.
    *
    * If calling this operation results in a mutation, a seperate updated state is emitted on the
    * current state flux.
    *
    * Returns the current set of VersionedSource
    */
   fun submitSources(
      submission: VersionedSourceSubmission,
      resultConsumer: ((result: Pair<SchemaSet, List<CompilationError>>) -> Unit) ?= null) {
      val identifier = submission.identifier
      val removedSchemaIds = when (val existingSubmission = sources[identifier]) {
         null -> {
            sources[identifier.publisherId] = submission
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
            sources[identifier.publisherId] = submission
            removedVersionedSourceIds
         }
      }
      sources[submission.identifier.publisherId] = submission
      emitRemovedSchemaIds(removedSchemaIds, resultConsumer)
      keepAliveStrategyMonitors
         .firstOrNull { it.appliesTo(submission.identifier.keepAlive) }
         ?.monitor(submission.identifier)
   }

   private fun emitRemovedSchemaIds(removedSchemaIds: List<SchemaId>, resultConsumer: ((result: Pair<SchemaSet, List<CompilationError>>) -> Unit) ?= null) {
      val currentSources = this.sources.values.flatMap { it.sources }
      val message = SourcesUpdatedMessage(currentSources, removedSchemaIds, resultConsumer)
      this.sink.emitNext(message, emitFailureHandler)
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
   val resultConsumer: ((result: Pair<SchemaSet, List<CompilationError>>) -> Unit) ?= null
)

