package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.JetInstance
import com.hazelcast.jet.Job
import io.vyne.pipelines.jet.api.JobStatus
import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.PipelineStatus
import io.vyne.pipelines.jet.api.RunningPipelineSummary
import io.vyne.pipelines.jet.api.SubmittedPipeline
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import mu.KotlinLogging
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct

@RestController
class PipelineService(
   private val pipelineManager: PipelineManager,
   private val pipelineRepository: PipelineRepository
) : PipelineApi {

   private val logger = KotlinLogging.logger {}

   @PostConstruct
   fun loadAndSubmitExistingPipelines(): List<Pair<SubmittedPipeline, Job>> {
      val loadedPipelines = pipelineRepository.loadPipelines()
      val errorCount = AtomicInteger(0)
      val submittedPipelines = loadedPipelines.mapNotNull { pipelineSpec ->
         try {
            pipelineManager.startPipeline(pipelineSpec)
         } catch (e: Exception) {
            logger.error(e) { "Loaded pipeline ${pipelineSpec.name} (${pipelineSpec.id}) failed to start" }
            errorCount.incrementAndGet()
            null
         }
      }
      logger.info { "Submitted ${submittedPipelines.size} pipelines successfully, with ${errorCount.get()} failures" }
      return submittedPipelines
   }

   @PostMapping("/api/pipelines")
   override fun submitPipeline(@RequestBody pipelineSpec: PipelineSpec<*, *>): Mono<SubmittedPipeline> {
      logger.info { "Received new pipelineSpec: \n${pipelineSpec}" }
      pipelineRepository.save(pipelineSpec)
      val (submittedPipeline, _) = pipelineManager.startPipeline(pipelineSpec)

      return Mono.just(submittedPipeline)
   }

   @GetMapping("/api/pipelines")
   override fun getPipelines(): Mono<List<RunningPipelineSummary>> {
      return Mono.just(pipelineManager.getPipelines())
   }

   @GetMapping("/api/pipelines/{pipelineSpecId}")
   override fun getPipeline(@PathVariable("pipelineSpecId") pipelineSpecId: String): Mono<RunningPipelineSummary> {
      return Mono.just(pipelineManager.getPipeline(pipelineSpecId))
   }


   @DeleteMapping("/api/pipelines/{pipelineId}")
   override fun deletePipeline(@PathVariable("pipelineId") pipelineSpecId: String): Mono<PipelineStatus> {
      val status = pipelineManager.deletePipeline(pipelineSpecId)
      if (status.status != JobStatus.RUNNING) {
         val pipeline = pipelineManager.getPipeline(pipelineSpecId)
         pipelineRepository.deletePipeline(pipeline.pipeline!!.spec)
      }
      return Mono.just(status)
   }

}

