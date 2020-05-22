package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.PIPELINE_METADATA_KEY
import io.vyne.pipelines.orchestrator.PipelineState.RUNNING
import io.vyne.pipelines.orchestrator.PipelineState.STARTING
import io.vyne.pipelines.orchestrator.pipelines.PipelineDeserialiser
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent
import org.springframework.context.ApplicationListener
import org.springframework.http.HttpEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Component
class PipelinesManager(val pipelineDeserialiser: PipelineDeserialiser,
                       val discoveryClient: DiscoveryClient) : ApplicationListener<InstanceRegisteredEvent<Any>> {


   // All pipeline runners instances
   lateinit var runnerInstances: List<ServiceInstance>

   // Current pipelines <Pipeline id, Pipeline state>
   val pipelines = HashMap<String, PipelineStateSnapshot>()

   /**
    * Add a pipeline to the global state. Perform some verification/validation here
    */
   fun addPipeline(pipelineDefinition: String) {
      val pipeline = pipelineDeserialiser.deserialise(pipelineDefinition)

      if (pipelines.containsKey(pipeline.id)) {
         throw PipelineAlreadyExistsException("Pipeline with id ${pipeline.id} already exists")
      }

      // if pipeline is valid, we schedule it
      schedulePipeline(pipelineDefinition)
   }

   /**
    * Schedule a pipeline for assignment to runners
    */
   fun schedulePipeline(pipelineDefinition: String) {
      val pipeline = pipelineDeserialiser.deserialise(pipelineDefinition)

      println("Scheduling pipeline " + pipeline.id)

      // Initiate state
      val pipelineState = PipelineStateSnapshot(pipeline, pipelineDefinition, null, PipelineState.SCHEDULED, "")
      pipelines[pipeline.id] = pipelineState

      // For now, the scheduling is just a synchronous method call.
      // In the future, there might be a asynchronous queue and another period process to run the pipelines
      runPipeline(pipeline.id)
   }

   /**
    * Select a runner instance and run the pipeline on it
    */
   fun runPipeline(pipelineId: String) {
      var pipelineState = pipelines[pipelineId]!! // FIXME
      try {

         var availableServers = runnerInstances.filter { it.metadata[PIPELINE_METADATA_KEY] == null }

         if (availableServers.isEmpty()) {
            pipelineState.info = "No Pipeline Runner available"
         } else {
            // For now, pick a server at random
            // In the future, the selection will be more elaborated and will involve tagging and capacity
            val runner = availableServers.random()
            submitPipelineToRunner(pipelineState, runner)
         }


      } catch (e: Exception) {
         pipelineState.info = e.message
         throw RuntimeException("Could not start pipeline", e)
      }
   }

   /**
    * Submit a pipeline to a runner
    */
   fun submitPipelineToRunner(pipelineState: PipelineStateSnapshot, runner: ServiceInstance) {

      val map = LinkedMultiValueMap<String, String>()
      map.add("content-type", "application/json")
      val request = HttpEntity(pipelineState.pipelineDescription, map)

      val endpoint = with(runner) { "http://$host:$port/runner/pipelines" }
      RestTemplate().postForEntity(endpoint, request, String::class.java)

      pipelineState.state = STARTING
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
   fun reloadState() {
      var previousPipelines = pipelines.map { it.value }

      // 1. Find all the runner instances
      runnerInstances = discoveryClient.getInstances("pipeline-runner")

      // 2. See what pipelines are currently running
      var runningPipelines = runnerInstances
         .map { it to it.metadata[PIPELINE_METADATA_KEY] } // only get the instances with pipeline metadata
         .filter { it.second != null }
         .map { // Save internal state of these running pipelines
            var pipeline = pipelineDeserialiser.deserialise(it.second!!)
            pipeline.id to PipelineStateSnapshot(pipeline, it.second!!, it.first, RUNNING)
         }.toMap()


      // Update the running pipelines
      pipelines.putAll(runningPipelines)

      // Schedule pipelines which must be started. Pipeline must be started if:
      previousPipelines
         .filter { !runningPipelines.containsKey(it.id) }  // he's not currently running
         .filter { it.state != STARTING } // and he's not currently starting
         .forEach { schedulePipeline(it.pipelineDescription) }

   }

   override fun onApplicationEvent(event: InstanceRegisteredEvent<Any>) {
      reloadState()
   }

}

enum class PipelineState {

   // Pipeline has been scheduled for assignment to a runner
   SCHEDULED,

   // Pipeline has been submitting to a runner, and we're waiting for startup
   STARTING,

   // Pipeline is running on a runner. For now, running means just that the pipeline has been sent to a runner, and the runner acknowledged the pipeline (write in its metadata)
   RUNNING
}
