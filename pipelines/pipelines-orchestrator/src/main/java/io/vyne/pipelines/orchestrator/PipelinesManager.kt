package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.PIPELINE_METADATA_KEY
import io.vyne.pipelines.orchestrator.PipelineState.RUNNING
import io.vyne.pipelines.orchestrator.PipelineState.STARTING
import io.vyne.pipelines.orchestrator.pipelines.PipelineDeserialiser
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.utils.log
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent
import org.springframework.context.ApplicationListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PipelinesManager(private val discoveryClient: DiscoveryClient,
                       private val pipelineRunnerApi: PipelineRunnerApi,
                       private val runningPipelineDiscoverer: RunningPipelineDiscoverer) : ApplicationListener<InstanceRegisteredEvent<Any>> {


   // All pipeline runners instances
   val runnerInstances = mutableListOf<ServiceInstance>()

   // Current pipelines
   val pipelines = mutableMapOf<String, PipelineStateSnapshot>()

   /**
    * Add a pipeline to the global state. Perform some verification/validation here
    */
   fun addPipeline(pipelineRef: PipelineReference) {


      if (pipelines.containsKey(pipelineRef.name)) {
         throw PipelineAlreadyExistsException("Pipeline with name ${pipelineRef.name} already exists")
      }

      // if pipeline is valid, we schedule it
      schedulePipeline(pipelineRef.name, pipelineRef.description)

   }

   /**
    * Schedule a pipeline for assignment to runners
    */
   private fun schedulePipeline(pipelineName: String, pipelineDefinition: String) {
      log().info("Scheduling pipeline $pipelineName")

      // Initiate state
      pipelines[pipelineName] = PipelineStateSnapshot(pipelineName, pipelineDefinition, null, PipelineState.SCHEDULED)

      // For now, the scheduling is just a synchronous method call.
      // In the future, there might be a asynchronous queue and another period process to run the pipelines
      runPipeline(pipelineName)
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
               pipelineRunnerApi.submitPipeline(pipelineSnapshot.pipelineDescription)
               pipelineSnapshot.state = STARTING
               pipelineSnapshot.info = "Pipeline sent to runner"
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

         // 1. Find all the runner instances
         runnerInstances.clear()
         runnerInstances.addAll(discoveryClient.getInstances("pipeline-runner"))

         // 2. See what pipelines are currently running
         val pipelineInstances = runningPipelineDiscoverer.discoverPipelines(runnerInstances)

         // 3. Overwrite the running pipelines
         val runningPipelines = pipelineInstances.map {
            it.key.name to PipelineStateSnapshot(it.key.name, it.key.description, it.value, RUNNING)
         }.toMap()

         pipelines.putAll(runningPipelines)

         // 4. Schedule pipelines which must be started.
         reschedulePipelines(previousPipelines, runningPipelines)

      } catch (e: Exception) {
         log().error("Error while reloading internal state", e)
      }
   }

   fun reschedulePipelines(previousPipelines: List<PipelineStateSnapshot>, runningPipelines: Map<String, PipelineStateSnapshot>) {
      previousPipelines // A pipeline must be rescheduled if:
         .filter { !runningPipelines.containsKey(it.name) }  // he's not currently running
         .filter { it.state != STARTING } // and he's not currently starting
         .forEach { schedulePipeline(it.name, it.pipelineDescription) }
   }

   /**
    * Immediately reload state after the orchestrator register itself to the discovery server
    */
   override fun onApplicationEvent(event: InstanceRegisteredEvent<Any>) {
      reloadState()
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
         else -> runnerInstance to metadatas.values.map { toPipelineReference(it) }
      }
   }

   private fun toPipelineReference(pipelineDescription: String): PipelineReference {
      val pipeline = pipelineDeserialiser.deserialise(pipelineDescription)
      return PipelineReference(pipeline.name, pipelineDescription)
   }

}

data class PipelineStateSnapshot(val name: String,
                                 val pipelineDescription: String,
                                 var instance: ServiceInstance?,
                                 var state: PipelineState,
                                 var info: String = "")

enum class PipelineState {

   // Pipeline has been scheduled for assignment to a runner
   SCHEDULED,

   // Pipeline has been submitting to a runner, and we're waiting for startup
   STARTING,

   // Pipeline is running on a runner. For now, running means just that the pipeline has been sent to a runner, and the runner acknowledged the pipeline (wrote it in its metadata)
   RUNNING
}

data class PipelineReference(val name: String, val description: String)

class PipelineAlreadyExistsException(message: String) : Exception(message)

