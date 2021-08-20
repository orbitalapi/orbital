package io.vyne.pipelines.jet.pipelines

import com.hazelcast.jet.JetInstance
import io.vyne.pipelines.PipelineSpec
import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.PipelineStatus
import io.vyne.pipelines.jet.api.RunningPipelineSummary
import io.vyne.pipelines.jet.api.SubmittedPipeline
import mu.KotlinLogging
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class PipelineService(
   private val pipelineManager: PipelineManager,
   private val jetInstance: JetInstance
) : PipelineApi {

   private val logger = KotlinLogging.logger {}

   @PostMapping("/api/pipelines")
   override fun submitPipeline(@RequestBody pipelineSpec: PipelineSpec<*, *>): Mono<SubmittedPipeline> {
      logger.info { "Received new pipelineSpec: \n${pipelineSpec}" }
      val (submittedPipeline, _) = pipelineManager.startPipeline(pipelineSpec)

      return Mono.just(submittedPipeline)
   }

   @GetMapping("/api/pipelines")
   override fun getPipelines(): Mono<List<RunningPipelineSummary>> {
      return Mono.just(pipelineManager.getPipelines())
   }

   @GetMapping("/api/pipelines/{pipelineSpecId}")
   override fun getPipeline(@PathVariable("pipelineSpecId") pipelineSpecId:String):Mono<RunningPipelineSummary> {
      return Mono.just(pipelineManager.getPipeline(pipelineSpecId))
   }


   @DeleteMapping("/api/pipelines/{pipelineId}")
   override fun deletePipeline(@PathVariable("pipelineId") pipelineSpecId: String): Mono<PipelineStatus> {
      return Mono.just(pipelineManager.deletePipeline(pipelineSpecId))
   }

}

