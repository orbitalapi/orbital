package com.orbitalhq.nebula

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VersionedSource
import com.orbitalhq.config.ConfigFileLocationConventions
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.service.annotation.DeleteExchange
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
class NebulaService(
   private val nebulaApi: NebulaApi,
   private val schemaWatcher: NebulaSchemaWatcher,
   private val envVariableSource: NebulaEnvVariableSource
) {

   private var currentStackState: NebulaStackState = emptyMap()
   private var currentEnvironmentVariables: Map<StackName, Map<ComponentType, Map<EnvVarKey, EnvVarValue>>> = emptyMap()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      logger.info { "Nebula service initialized" }
      schemaWatcher.stacksUpdated.subscribe { event ->
         val added = createOrUpdateStacks(event.added)
         val deleted = deleteRemovedStacks(event.removed)
         val updated = createOrUpdateStacks(event.updated.mapValues { (k, v) -> v.leftValue() })

         Flux.merge(added + deleted + updated)
            .subscribe {
               updateCurrentStackSet()
            }
      }
   }

   private fun updateCurrentStackSet() {
      nebulaApi.getStacks().subscribe { stacks ->
         this.currentStackState = stacks
         this.currentEnvironmentVariables = envVariableSource.updateEnvVariables(currentStackState)
      }
   }

   @GetMapping("/api/stubs")
   fun getNebulaStack(): NebulaStacksResponse {
      return NebulaStacksResponse(
         currentStackState,
         currentEnvironmentVariables
      )
   }

   /**
    * Converts the packageQualifiedName of the file to something
    * slightly more human-readable.
    *
    * Just removes the config file path and the file suffix
    */
   private fun toStackName(packageIdentifier: PackageIdentifier?, fileName: String): String {
      val directory = ConfigFileLocationConventions.OrbitalNebulaPathEntry.split("/").dropLast(1)
         .joinToString("/")
      val stackName = fileName
         .replace("${directory}/", "")
         .removeSuffix(".nebula.kts")

      val prefix = packageIdentifier?.let { "[${it.organisation}:${it.name}]/" }
      return "${prefix.orEmpty()}$stackName"
   }

   private fun deleteRemovedStacks(removed: Map<String, VersionedSource>): List<Mono<out Any>> {
      return removed.values.map { source ->
         val stackName = toStackName(source.packageIdentifier, source.name)
         logger.info { "Removing deleted Nebula stack for source $stackName" }
         try {
            nebulaApi.deleteStack(stackName)
               .subscribeOn(Schedulers.boundedElastic())
               .doOnError { e ->
                  logger.error(e) { "Error while deleting nebula stack for $stackName" }
               }
               .doOnNext {
                  logger.info { "Stack for $stackName updated successfully" }
               }

         } catch (e: Exception) {
            logger.error(e) { "Error while deleting nebula stack for $stackName" }
            Mono.error<String>(e)
         }
      }
   }

   private fun createOrUpdateStacks(added: Map<String, VersionedSource>): List<Mono<String>> {
      return added.values.map { source ->
         val stackName = toStackName(source.packageIdentifier, source.name)
         logger.info { "Submitting Nebula stack for source $stackName" }
         try {
            nebulaApi.updateStack(stackName, source.content)
               .subscribeOn(Schedulers.boundedElastic())
               .doOnError { e ->
                  logger.error(e) { "Error while updating nebula stack for $stackName" }
               }
               .doOnNext {
                  logger.info { "Stack for $stackName updated successfully" }
               }

         } catch (e: Exception) {
            logger.error(e) { "Error while updating nebula stack for $stackName" }
            Mono.error<String>(e)
         }
      }
   }
}

data class NebulaStacksResponse(
   val stacks: NebulaStackState,
   val environmentVariables: Map<StackName, Map<ComponentType, Map<EnvVarKey, EnvVarValue>>>
)

typealias NebulaStackState = Map<String, Map<String, ComponentInfo>>
typealias StackName = String
typealias ComponentType = String
typealias EnvVarKey = String
typealias EnvVarValue = String

/**
 * Note that the nebula entry here should be resolved via services.conf
 */
@HttpExchange("http://nebula")
interface NebulaApi {
   /**
    * Returns the names of current stacks
    */
   @GetExchange("/stacks")
   fun getStacks(): Mono<NebulaStackState>

   /**
    * Creates a new stack.
    * Returns the newly created stack name
    */
   @PostExchange("/stacks")
   fun postStack(@RequestBody scriptSource: String): Mono<String>

   /**
    * Updates an existing stack.
    * Returns the updated stacks name
    */
   @PutExchange("/stacks/{name}")
   fun updateStack(@PathVariable("name") name: String, @RequestBody script: String): Mono<String>

   /**
    * Deletes a stack.
    * Returns the list of remaining stack names.
    */
   @DeleteExchange("/stacks/{name}")
   fun deleteStack(@PathVariable("name") name: String): Mono<List<String>>
}

// copy of Nebula's Component info
data class ComponentInfo(
   val container: ContainerInfo?,
   val componentConfig: Map<String, Any>
)

data class ContainerInfo(
   val containerId: String,
   val imageName: String,
   val containerName: String,
)
