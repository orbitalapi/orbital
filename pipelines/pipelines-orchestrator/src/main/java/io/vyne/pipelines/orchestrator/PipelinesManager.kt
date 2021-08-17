package io.vyne.pipelines.orchestrator

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.pipelines.PIPELINE_METADATA_KEY
import io.vyne.pipelines.PipelineTransportHealthMonitor
import io.vyne.pipelines.orchestrator.PipelineState.RUNNING
import io.vyne.pipelines.orchestrator.PipelineState.STARTING
import io.vyne.pipelines.orchestrator.pipelines.PipelineDeserialiser
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.pipelines.runner.PipelineStatusUpdate
import io.vyne.utils.log
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent
import org.springframework.context.ApplicationListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Component
class PipelinesManager(
   private val discoveryClient: DiscoveryClient,
   private val pipelineRunnerApi: PipelineRunnerApi,
   private val runningPipelineDiscoverer: RunningPipelineDiscoverer,
   private val objectMapper: ObjectMapper,
   @Value("\${vyne.pipelineRunnerService.name:pipeline-runner}") private val pipelineRunnerServiceName: String
) : ApplicationListener<InstanceRegisteredEvent<Any>> {


   // All pipeline runners instances
   val runnerInstances = mutableListOf<ServiceInstance>()

   // Current pipelines
   val pipelines = mutableMapOf<String, PipelineStateSnapshot>()

   // Prepping for a demo, and need to get removal working.  This is a real fucker.
   val removedPipelines = mutableListOf<String>()

   /**
    * Add a pipeline to the global state. Perform some verification/validation here
    */
   fun addPipeline(pipelineRef: PipelineReference): PipelineStateSnapshot {


      if (pipelines.containsKey(pipelineRef.name)) {
         throw PipelineAlreadyExistsException("Pipeline with name ${pipelineRef.name} already exists")
      }

      // if pipeline is valid, we schedule it
      return schedulePipeline(pipelineRef.name, pipelineRef.description)
   }

   fun removePipeline(name: String): Mono<PipelineStatusUpdate> {
      log().info("Submitting removal of pipeline $name to runner")
      return pipelineRunnerApi.removePipeline(name)
         .map { status ->
            log().info("Pipeline $name removed from pipeline runner")
            this.removedPipelines.add(name)
            this.pipelines.remove(name)
            status
         }
   }

   /**
    * Schedule a pipeline for assignment to runners
    */
   private fun schedulePipeline(pipelineName: String, pipelineDefinition: String): PipelineStateSnapshot {
      log().info("Scheduling pipeline $pipelineName")

      // Initiate state
      val pipelineState = PipelineStateSnapshot(pipelineName, pipelineDefinition, null, PipelineState.SCHEDULED)
      pipelines[pipelineName] = pipelineState
      // For now, the scheduling is just a synchronous method call.
      // In the future, there might be an asynchronous queue and another period process to run the pipelines
      runPipeline(pipelineName)

      return pipelineState
   }

   /**
    * Select a runner instance and run the pipeline on it
    */
   private fun runPipeline(pipelineName: String) {
      val pipelineSnapshot = pipelines[pipelineName]!!
      try {

         when (runnerInstances.size) {
            0 -> {
               log().info("No runners available")
               pipelineSnapshot.info = "No Pipeline Runner available"
            }

            else -> {
               // For now, pick a runner at using load balancing
               // In the future, the selection will be more elaborated and can involve tagging and capacity
               // We have block() here rather than invoking subscribe() otherwise rest call might fail in the background
               // and we'll set the pipeline state as STARTING incorrectly!
               pipelineRunnerApi.submitPipeline(pipelineSnapshot.pipelineDescription).subscribe {
                  pipelineSnapshot.state = STARTING
                  pipelineSnapshot.info = "Pipeline sent to runner"
               }
            }
         }

      } catch (e: Exception) {
         pipelineSnapshot.info = e.message ?: e.toString()
         throw RuntimeException("Could not start pipeline", e)
      }
   }

   /**
    * Periodically refresh the state
    */
   @Scheduled(fixedRate = 5000)
   fun getServers() = reloadState()

   /**
    * Reloads the internal state of the Orchestrator
    * - Discover all pipeline-runner services
    * - Lookup metadata to see what they are running
    * - Diff with the previous running pipelines to determine if some pipelines must be restarted
    */
   @Synchronized
   fun reloadState() {
      try {
         val previousPipelines = pipelines.map { it.value }
         logger.info {
            "Current pipelines: $previousPipelines}"
         }
         // 1. Find all the runner instances
         runnerInstances.clear()
         runnerInstances.addAll(discoveryClient.getInstances(pipelineRunnerServiceName))

         logger.info {
            "Available Runners: ${runnerInstances.map { "${it.instanceId}, ${it.host}: ${it.port}" }}"
         }

         // 2. See what pipelines are currently running
         val pipelineInstances = runningPipelineDiscoverer.discoverPipelines(runnerInstances)
         logger.info {
            "active pipelines (starting or running): ${pipelineInstances.map { "${it.key.name} at ${it.value.host}: ${it.value.port}" }}"
         }

         // 3. Overwrite the running pipelines
         val runningPipelines = pipelineInstances.map {
            val runner = objectMapper.convertValue(it.value, PipelineRunnerInstance::class.java)
            it.key.name to PipelineStateSnapshot(it.key.name, it.key.description, runner, RUNNING)
         }.toMap()

         pipelines.putAll(runningPipelines)

         // 4. Schedule pipelines which must be started.
         reschedulePipelines(previousPipelines, runningPipelines)

      } catch (e: Exception) {
         log().error("Error while reloading internal state", e)
      }
   }

   fun reschedulePipelines(
      previousPipelines: List<PipelineStateSnapshot>,
      runningPipelines: Map<String, PipelineStateSnapshot>
   ) {
      previousPipelines // A pipeline must be rescheduled if:
         .filter { !runningPipelines.containsKey(it.name) }  // he's not currently running
         .filter { !removedPipelines.contains(it) } // It hasn't been explicilty removed
         .filter { it.state != STARTING } // and he's not currently starting
         .forEach { schedulePipeline(it.name, it.pipelineDescription) }
   }

   /**
    * Immediately reload state after the orchestrator register itself to the discovery server
    */
   override fun onApplicationEvent(event: InstanceRegisteredEvent<Any>) {
      reloadState()
   }

   fun activePipelines(): List<PipelineStateSnapshot> {
      return this.pipelines
         .filter { (key,value) -> !this.removedPipelines.contains(key) }
         .values.toList()
   }

}

@Component
class RunningPipelineDiscoverer(val pipelineDeserialiser: PipelineDeserialiser) {

   /**
    * Returns all the pipelines running on a specific set of instances
    */
   fun discoverPipelines(runnerInstances: List<ServiceInstance>): Map<PipelineReference, ServiceInstance> {
      return runnerInstances.mapNotNull { extractRunnerMetadata(it) }
         .flatMap {
            it.second.map { ref -> ref to it.first }
         }.toMap()
   }

   /**
    * Extract the pipeline information from a service instance
    */
   private fun extractRunnerMetadata(runnerInstance: ServiceInstance): Pair<ServiceInstance, List<PipelineReference>>? {
      val metadatas = runnerInstance.metadata.filter { it.key.startsWith(PIPELINE_METADATA_KEY) }
      return when (metadatas.size) {
         0 -> null
         else -> runnerInstance to metadatas.values.mapNotNull { toPipelineReference(it) }
      }
   }

   private fun toPipelineReference(pipelineDescription: String): PipelineReference? {
      if (pipelineDescription == PipelineTransportHealthMonitor.PipelineTransportStatus.TERMINATED.name) {
         return null
      }
      val pipeline = pipelineDeserialiser.deserialise(pipelineDescription)
      return PipelineReference(pipeline.name, pipelineDescription)
   }

}


data class PipelineReference(val name: String, val description: String)

class PipelineAlreadyExistsException(message: String) : Exception(message)

