package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.PIPELINE_METADATA_KEY
import io.vyne.pipelines.Pipeline
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
class PipelinesManager(private val pipelineDeserialiser: PipelineDeserialiser,
                       private val discoveryClient: DiscoveryClient,
                       private val pipelineRunnerApi: PipelineRunnerApi) : ApplicationListener<InstanceRegisteredEvent<Any>> {


   // All pipeline runners instances
   var runnerInstances: List<ServiceInstance> = ArrayList()

   // Current pipelines <Pipeline name, Pipeline state>
   val pipelines = HashMap<String, PipelineStateSnapshot>()

   /**
    * Add a pipeline to the global state. Perform some verification/validation here
    */
   fun addPipeline(pipelineDefinition: String): Pipeline {

      // Deserialise the full pipeline. We only need the name for now. But it allows us to validate the json and in the future, perform some validations
      val pipeline = pipelineDeserialiser.deserialise(pipelineDefinition)

      if (pipelines.containsKey(pipeline.name)) {
         throw PipelineAlreadyExistsException("Pipeline with name ${pipeline.name} already exists")
      }

      // if pipeline is valid, we schedule it
      schedulePipeline(pipeline.name, pipelineDefinition)

      return pipeline
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
      var pipelineSnapshot = pipelines[pipelineName]!!
      try {

         val availableServers = runnerInstances.filter { it.metadata[PIPELINE_METADATA_KEY] == null }

         when (availableServers.size) {
            0 -> pipelineSnapshot.info = "No Pipeline Runner available"

            // For now, pick a server at random
            // In the future, the selection will be more elaborated and can involve tagging and capacity
            else -> {
               val runner = availableServers.random()
               val endpoint = with(runner) { "http://$host:$port/runner/pipelines" }
               pipelineRunnerApi.submitPipeline(endpoint, pipelineSnapshot.pipelineDescription)
               pipelineSnapshot.state = STARTING
               pipelineSnapshot.instance = runner
               pipelineSnapshot.info = "Assigning to ${runner.instanceId}"
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
         var previousPipelines = pipelines.map { it.value }

         // 1. Find all the runner instances
         runnerInstances = discoveryClient.getInstances("pipeline-runner")

         // 2. See what pipelines are currently running
         var runningPipelines = runnerInstances
            .map { it to it.metadata[PIPELINE_METADATA_KEY] } // only get the instances running a pipelines
            .filter { it.second != null }
            .map { it.first to it.second!! } // (Maybe a better way to do that ?)
            .map { // Save internal state of these running pipelines
               var pipeline = pipelineDeserialiser.deserialise(it.second)
               pipeline.name to PipelineStateSnapshot(pipeline.name, it.second, it.first, RUNNING)
            }.toMap()


         // 3. Overwrite the running pipelines
         pipelines.putAll(runningPipelines)

         // 4. Schedule pipelines which must be started. A pipeline must be started if:
         previousPipelines
            .filter { !runningPipelines.containsKey(it.name) }  // he's not currently running
            .filter { it.state != STARTING } // and he's not currently starting
            .forEach { schedulePipeline(it.name, it.pipelineDescription) }

      } catch (e: Exception) {
         log().error("Error while reloading internal state", e)
      }

   }

   /**
    * Immediately reload state when the orchestrator register itself
    */
   override fun onApplicationEvent(event: InstanceRegisteredEvent<Any>) {
      reloadState()
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

class PipelineAlreadyExistsException(message: String) : Exception(message)

