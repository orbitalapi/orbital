package io.vyne.schema.publisher

import arrow.core.extensions.list.functorFilter.filter
import io.vyne.PackageIdentifier
import io.vyne.SourcePackage
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


data class PublisherHealth(
   val status: Status,
   val message: String? = null,
   val timestamp: Instant = Instant.now()
) {
   enum class Status {
      Healthy,
      Unhealthy,
      Unknown
   }
}

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
   internal val packages: ConcurrentMap<PublisherId, SourcePackage> = ConcurrentHashMap()
) {
   private val logger = KotlinLogging.logger { }
   private val transportConnectionIdMap = ConcurrentHashMap<TransportConnectionId, PublisherId>()
   private val healthIndicators = ConcurrentHashMap<PackageIdentifier, PublisherHealth>()
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
            markPublisherAsUnhealthy(zombieSource.publisherId)
         }
      }
   }

   fun associateConnectionToPublisher(connectionId: TransportConnectionId, publisherId: PublisherId) {
      transportConnectionIdMap[connectionId] = publisherId
   }

   fun markPackagesForTransportAsUnhealthy(connectionId: TransportConnectionId, reason: String? = null) {
      transportConnectionIdMap.remove(connectionId)?.let { publisherId: PublisherId ->
         markPublisherAsUnhealthy(publisherId, reason)
      }
   }

   fun markPublisherAsUnhealthy(publisherId: String, reason: String? = null) {
      val affectedPackage = packages[publisherId]?.packageMetadata
      if (affectedPackage == null) {
         logger.warn { "Can't mark publisher $publisherId as unhealthy, as they weren't found in the local registry" }
         return
      }
      healthIndicators[affectedPackage.identifier] =
         PublisherHealth(status = PublisherHealth.Status.Unhealthy, message = reason)
      logger.info { "Package ${affectedPackage.identifier} marked as unhealthy." }

   }

   fun removeSources(publisherId: String, emitUpdateMessage: Boolean = true): PackagesUpdatedMessage? {
      val updateMessage = packages.remove(publisherId)?.let { sourcePackage ->
         buildAndEmitUpdateMessage(listOf(PackageRemoved(sourcePackage)), emitUpdateMessage)
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
      val unversionedId = submission.sourcePackage.identifier.unversionedId
      val existingPackages =
         this.packages.filter { (key, sourcePackage) -> sourcePackage.identifier.unversionedId == unversionedId }
      if (existingPackages.size > 1) {
         logger.warn { "Found multiple pacakages matching $unversionedId - that's unexpected'" }
      }
      val deltas = if (existingPackages.isEmpty()) {
         listOf(PackageAdded(submission.sourcePackage))
      } else {
         existingPackages.map { (publisher, sourcePackage) ->
            logger.info { "Package ${sourcePackage.identifier} (published by $publisher) will be replaced by ${submission.sourcePackage.identifier} published by ${submission.publisherId}" }
            val oldVesion = packages.remove(publisher)!!
            PackageUpdated(oldVesion, submission.sourcePackage)
         }
      }


      packages[submission.publisherId] = submission.sourcePackage
      healthIndicators[submission.sourcePackage.identifier] = PublisherHealth(PublisherHealth.Status.Healthy)
      notifyKeepAlive(submission)

      return buildAndEmitUpdateMessage(deltas, true)
   }

   private fun notifyKeepAlive(submission: KeepAlivePackageSubmission) {
      keepAliveStrategyMonitors
         .filter { it.appliesTo(submission.keepAlive) }
         .forEach { it.monitor(submission.publisherConfig()) }
   }

   private fun buildSourcesUpdatesMessage(deltas: List<PackageDelta>): PackagesUpdatedMessage {
      val currentSources = packages.values.toList()
      return PackagesUpdatedMessage(currentSources, deltas)
   }

   private fun buildAndEmitUpdateMessage(
      deltas: List<PackageDelta>,
      emit: Boolean
   ): PackagesUpdatedMessage {
      val message = buildSourcesUpdatesMessage(deltas)
      if (emit) {
         this.sink.emitNext(message, emitFailureHandler)
      }
      return message
   }

   fun getPublisherHealth(identifier: PackageIdentifier): PublisherHealth {
      return this.healthIndicators[identifier] ?: PublisherHealth(PublisherHealth.Status.Unknown)
   }
}



