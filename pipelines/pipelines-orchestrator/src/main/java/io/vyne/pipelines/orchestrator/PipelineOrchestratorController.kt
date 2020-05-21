package io.vyne.pipelines.orchestrator

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.pipelines.PIPELINE_METADATA_KEY
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.orchestrator.PipelineState.*
import io.vyne.pipelines.orchestrator.pipelines.InvalidPipelineDescriptionException
import io.vyne.pipelines.orchestrator.pipelines.PipelineDeserialiser
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.pipelines.runner.PipelineInstanceReference
import io.vyne.utils.log
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent
import org.springframework.context.ApplicationListener
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.http.ResponseEntity.ok
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PipelineOrchestratorController(val pipelineManager: PipelinesManager) {

   @PostMapping("/runner/pipelines")
   fun submitPipeline(@RequestBody pipelineDefinition: String): ResponseEntity<Any> {
      log().info("Received submitted pipeline: \n$pipelineDefinition")



      return try {
         var instanceRef = pipelineManager.addPipeline(pipelineDefinition)
         ok(instanceRef)
      } catch (e: InvalidPipelineDescriptionException) {
         badRequest().body(e.message)
      }
   }

   @GetMapping("/runners")
   fun getInstances(): ResponseEntity<Any> {

      return try {
         var instances = pipelineManager.runnerInstances
         ok(instances)
      } catch (e: Exception) {
         badRequest().body(e.message)
      }
   }

   @GetMapping("/runner/pipelines")
   fun getPipelines(): ResponseEntity<Any> {

      return try {
         var pipelines = pipelineManager.pipelines.map { it.value }
         ok(pipelines)
      } catch (e: Exception) {
         badRequest().body(e.message)
      }
   }
}

class PipelineAlreadyExistsException(message: String) : Exception(message)

data class PipelineStateSnapshot(val pipeline: Pipeline, val pipelineDescription: String, var instance: ServiceInstance?, var state: PipelineState, var info: String? = "") {
   val id = pipeline.id
}

enum class PipelineState {

   // Pipeline has been scheduled for assignment to a runner
   SCHEDULED,

   // Pipeline has been submitting to a runner, and we're waiting for startup
   STARTING,

   // Pipeline is running on a runner. For now, running means just that the pipeline has been sent to a runner, and the runner acknowledged the pipeline (write in its metadata)
   RUNNING
}

@Component
class PipelinesManager(val pipelineDeserialiser: PipelineDeserialiser,
                       val pipelineRunnerApi: PipelineRunnerApi,
                       val discoveryClient: DiscoveryClient) : ApplicationListener<InstanceRegisteredEvent<Any>> {


   // All pipeline runners instances
   lateinit var runnerInstances: List<ServiceInstance>

   // Current pipelines <Pipeline id, Pipeline state>
   val pipelines = HashMap<String, PipelineStateSnapshot>()

   /**
    * Add a pipeline to the global state. Perform some verification/validation here
    */
   fun addPipeline(pipelineDefinition: String): PipelineInstanceReference {
      val pipeline = pipelineDeserialiser.deserialise(pipelineDefinition)

      if (pipelines.containsKey(pipeline.id)) {
         throw PipelineAlreadyExistsException("Pipeline with id ${pipeline.id} already exists")
      }

      // if pipeline is valid, we schedule it
      return schedulePipeline(pipelineDefinition)
   }

   /**
    * Schedule a pipeline for assignment to runners
    */
   fun schedulePipeline(pipelineDefinition: String): PipelineInstanceReference {
      val pipeline = pipelineDeserialiser.deserialise(pipelineDefinition)

      println("Scheduling pipeline " + pipeline.id)

      // Initiate state
      val pipelineState = PipelineStateSnapshot(pipeline, pipelineDefinition, null, SCHEDULED, "")
      pipelines[pipeline.id] = pipelineState

      // For now, the scheduling is just a synchronous method call.
      // In the future, there might be a asynchronous queue and another period process to run the pipelines
      return runPipeline(pipeline.id)
   }

   /**
    * Actually run a pipeline on a runner
    */
   fun runPipeline(pipelineId: String): PipelineInstanceReference {
      var pipelineState = pipelines[pipelineId]!!
      try {
         return pipelineRunnerApi.submitPipeline(pipelineState.pipelineDescription)
            .also { pipelineState.state = STARTING }
      } catch (e: Exception) {
         pipelineState.info = e.message
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
   fun reloadState() {
      var previousPipelines = pipelines.map { it.value }

      // 1. Find all instances
      runnerInstances = discoveryClient.getInstances("pipeline-runner")

      // 2. See what pipelines are currently running
      var runningPipelines = HashMap<String, PipelineStateSnapshot>()

      runnerInstances
         .map { it to it.metadata[PIPELINE_METADATA_KEY] } // only get the instances with pipeline metadata
         .filter { it.second != null }
         .forEach { // Save internal state of these running pipelines
            var pipeline = pipelineDeserialiser.deserialise(it.second!!)
            runningPipelines.put(pipeline.id, PipelineStateSnapshot(pipeline, it.second!!, it.first, RUNNING))
         }


      // Update the running pipelines
      pipelines.putAll(runningPipelines)

      // Start pipelines which must be started. Pipeline must be started if:
      previousPipelines
         .filter { !runningPipelines.containsKey(it.id) }  // he's not currently running
         .filter { it.state != STARTING } // and he's not currently starting
         .forEach { schedulePipeline(it.pipelineDescription) }

   }

   override fun onApplicationEvent(event: InstanceRegisteredEvent<Any>) {
      reloadState()
   }

}




