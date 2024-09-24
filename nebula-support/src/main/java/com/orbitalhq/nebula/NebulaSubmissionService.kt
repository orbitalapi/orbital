package com.orbitalhq.nebula

import com.google.common.base.Throwables
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VersionedSource
import com.orbitalhq.config.ConfigFileLocationConventions
import com.orbitalhq.nebula.core.NebulaEnvVariablesMap
import com.orbitalhq.nebula.core.NebulaStackState
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import reactor.util.retry.Retry
import java.time.Duration

/**
 * Listens to the NebulaSchemaWatcher for updates, and submits them
 * to Nebula, fetching the current state
 */
@Component
class NebulaSubmissionService(
   private val nebulaApi: NebulaApi,
   private val schemaWatcher: NebulaSchemaWatcher,
   private val envVariableSource: NebulaEnvVariableSource,
   private val retryDuration: Duration = Duration.ofSeconds(5)
) {

   private val stateUpdatesSink = Sinks.many().multicast().directAllOrNothing<NebulaStateUpdatedEvent>()
   val stateUpdates: Flux<NebulaStateUpdatedEvent> = stateUpdatesSink.asFlux()

   init {
      logger.info { "Nebula submission service initialized" }
//      schemaWatcher.stacksUpdated.subscribe { event ->
//         logger.info { "Stacks updated" }
//         val added = createOrUpdateStacks(event.added)
//         val deleted = deleteRemovedStacks(event.removed)
//         val updated = createOrUpdateStacks(event.updated.mapValues { (k, v) -> v.leftValue() })
//
//         Flux.merge(added + deleted + updated)
//            .subscribe {
//               updateCurrentStackSet()
//            }
//      }
   }

   private fun deleteRemovedStacks(removed: Map<String, VersionedSource>): List<Mono<out Any>> {
      return removed.values.map { source ->
         val stackName = stackNameForSource(source)
         // Use Mono.defer() so that retries call the method again
         Mono.defer {
            logger.info { "Attempting to remove deleted Nebula stack for source $stackName" }
            nebulaApi.deleteStack(stackName)
         }

            .doOnError { e ->
               val errorMessage = Throwables.getRootCause(e).message
               logger.warn { "Error while deleting nebula stack for $stackName - $errorMessage" }
               stateUpdatesSink.tryEmitNext(
                  NebulaStateUpdatedEvent(
                     error = "Error deleting stack: ${e.message}",
                     hasPendingUpdates = true
                  )
               )
            }
            // Using fixedDelay, rather than BackOff(), as
            // tests with virtual time don't work with BackOff, and I'm actually
            // ok with consistent retries
            .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, retryDuration))
            .doOnNext {
               logger.info { "Stack for $stackName removed successfully" }
            }
            .subscribeOn(Schedulers.boundedElastic())
      }
   }

   private fun createOrUpdateStacks(added: Map<String, VersionedSource>): List<Mono<String>> {
      return added.values.map { source ->
         val stackName = stackNameForSource(source)
         // Use Mono.defer() so that retries call the method again
         Mono.defer {
            logger.info { "Attempting to submit nebula stack for source $stackName" }
            nebulaApi.updateStack(stackName, source.content)
         }

            .doOnError { e ->
               val errorMessage = Throwables.getRootCause(e).message
               logger.warn { "Error while updating nebula stack for $stackName - $errorMessage" }
               stateUpdatesSink.tryEmitNext(
                  NebulaStateUpdatedEvent(
                     error = "Error updating stack: ${e.message}",
                     hasPendingUpdates = true
                  )
               )
            }
            // Using fixedDelay, rather than BackOff(), as
            // tests with virtual time don't work with BackOff, and I'm actually
            // ok with consistent retries
            .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, retryDuration))
            .doOnNext {
               logger.info { "Stack for $stackName updated successfully" }
            }
            .subscribeOn(Schedulers.boundedElastic())
      }
   }

   private fun updateCurrentStackSet() {
      Mono.defer {
         logger.info { "Fetching current Nebula state" }
         nebulaApi.getStacks()
      }
         .doOnError { e ->
            val errorMessage = Throwables.getRootCause(e).message
            logger.warn { "Error while updating fetching current stacks: $errorMessage" }
            stateUpdatesSink.tryEmitNext(
               NebulaStateUpdatedEvent(
                  hasPendingUpdates = false,
                  error = "Error fetching stack state: $errorMessage",
               )
            )
         }
         .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, retryDuration))
         .subscribe { stackState ->
            val envVariables = envVariableSource.updateEnvVariables(stackState)
            stateUpdatesSink.tryEmitNext(
               NebulaStateUpdatedEvent(
                  stacks = stackState,
                  environmentVariables = envVariables,
                  hasPendingUpdates = false
               )
            ) // Emit success event
         }
   }

   fun emitStateUpdate(stateUpdatedEvent: NebulaStateUpdatedEvent) {
      this.stateUpdatesSink.tryEmitNext(stateUpdatedEvent)
   }


   companion object {
      private val logger = KotlinLogging.logger {}
      /**
       * Converts the packageQualifiedName of the file to something
       * slightly more human-readable.
       *
       * Just removes the config file path and the file suffix
       */
      fun stackNameForSource(source:VersionedSource): String {
         val directory = ConfigFileLocationConventions.OrbitalNebulaPathEntry.split("/").dropLast(1)
            .joinToString("/")
         val stackName = source.name
            .replace("${directory}/", "")
            .removeSuffix(".nebula.kts")

         val prefix = source.packageIdentifier?.let { "[${it.organisation}:${it.name}]/" }
         return "${prefix.orEmpty()}$stackName"
      }
   }

}

data class NebulaStateUpdatedEvent(
   val error: String? = null,
   val stacks: NebulaStackState = emptyMap(),
   val environmentVariables: NebulaEnvVariablesMap = emptyMap(),
   val hasPendingUpdates: Boolean
) {
   val hasError: Boolean
      get() {
         return error != null
      }

   companion object {
      fun empty(): NebulaStateUpdatedEvent {
         return NebulaStateUpdatedEvent(null, emptyMap(), emptyMap(), false)
      }
   }
}
