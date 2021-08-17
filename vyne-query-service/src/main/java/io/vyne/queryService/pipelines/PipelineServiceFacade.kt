package io.vyne.queryService.pipelines

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.netflix.hystrix.exception.HystrixRuntimeException
import feign.FeignException
import io.vyne.pipelines.orchestrator.PipelineRunnerInstance
import io.vyne.pipelines.orchestrator.PipelineStateSnapshot
import io.vyne.pipelines.orchestrator.PipelinesOrchestratorApi
import io.vyne.queryService.BadRequestException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Query server facade which forwards requests onto the pipeline orchestrator
 */
@RestController
class PipelineServiceFacade(private val orchestratorApi: PipelinesOrchestratorApi) {

   @PostMapping("/api/pipelines")
   fun submitPipeline(@RequestBody pipelineDescription: String): Mono<PipelineStateSnapshot> =
      handleFeignErrors {
         orchestratorApi.submitPipeline(pipelineDescription)
            .onErrorMap { e ->
               fun mapError(e: Throwable): Nothing {
                  when (e) {
                     is HystrixRuntimeException -> mapError(e.cause!!)
                     is FeignException.BadRequest -> {
                        val errorPayload = jacksonObjectMapper().readValue<Map<String,Any>>(e.contentUTF8())
                        val errorMessage = errorPayload["message"] as String? ?: e.message ?: e.contentUTF8()
                        throw BadRequestException(errorMessage)
                     }
                     else -> throw e
                  }
               }
               mapError(e)
            }
      }

   @GetMapping("/api/pipelines/runner")
   fun getRunners(): Mono<List<PipelineRunnerInstance>> = handleFeignErrors { orchestratorApi.getRunners() }

   @GetMapping("/api/pipelines")
   fun getPipelines(): Mono<List<PipelineStateSnapshot>> = handleFeignErrors { orchestratorApi.getPipelines() }

   @DeleteMapping("/api/pipelines/{pipelineName}")
   fun removePipeline(@PathVariable("pipelineName") pipelineName: String) = handleFeignErrors { orchestratorApi.removePipeline(pipelineName) }

   fun <T> handleFeignErrors(method: () -> T): T {
      try {
         return method.invoke()
      } catch (e: FeignException.BadRequest) {
         throw BadRequestException(e.message ?: e.contentUTF8())
      }
   }
}
