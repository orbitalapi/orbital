package io.vyne.pipelines.jet.pipelines

import io.vyne.pipelines.jet.api.JobStatus
import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.PipelineStatus
import io.vyne.pipelines.jet.api.RunningPipelineSummary
import io.vyne.pipelines.jet.api.SubmittedPipeline
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import javax.annotation.PostConstruct

suspend fun <T> retryWithBackOff(
   times: Int = Int.MAX_VALUE,
   initialDelay: Long = 500,
   maxDelay: Long = 30000,
   factor: Double = 2.0,
   block: suspend (exception: Throwable?, isLastAttempt: Boolean) -> T
): T {
   var exception: Throwable? = null
   var currentDelay = initialDelay
   repeat(times - 1) {
      try {
         return block(exception, false)
      } catch (e: Throwable) {
         // you can log an error here and/or make a more finer-grained
         // analysis of the cause to see if retry is needed
         exception = e
      }
      delay(currentDelay)
      currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
   }
   return block(exception, true) // last attempt
}


@RestController
class PipelineService(
   private val pipelineManager: PipelineManager,
   private val pipelineRepository: PipelineRepository
) : PipelineApi {

   private val logger = KotlinLogging.logger {}

   @PostConstruct
   fun loadAndSubmitExistingPipelines() {
      val loadedPipelines = pipelineRepository.loadPipelines()
      logger.info("Found ${loadedPipelines.size} pipelines. Submitting them. ")
      loadedPipelines.forEach { pipelineSpec ->
         GlobalScope.launch {
            submitLoadedPipeline(pipelineSpec)
         }
      }
   }

   private suspend fun submitLoadedPipeline(pipelineSpec: PipelineSpec<*, *>) {
      retryWithBackOff { exception, isLastAttempt ->
         if (exception != null) {
            val tryAgainText = if (isLastAttempt) "Trying once more before giving up." else "Trying again."
            logger.error(exception) { "Loaded pipeline ${pipelineSpec.name} (${pipelineSpec.id}) failed to start. $tryAgainText" }
         }
         logger.info("Submitting pipeline ${pipelineSpec.name}.")
         pipelineManager.startPipeline(pipelineSpec)
      }
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

