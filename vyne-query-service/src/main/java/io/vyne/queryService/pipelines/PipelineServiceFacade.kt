package io.vyne.queryService.pipelines

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.netflix.hystrix.exception.HystrixRuntimeException
import feign.FeignException
import io.vyne.pipelines.PipelineSpec
import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.RunningPipelineSummary
import io.vyne.pipelines.jet.api.SubmittedPipeline
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
class PipelineServiceFacade(private val pipelineApi: PipelineApi) {

   @PostMapping("/api/pipelines")
   fun submitPipeline(@RequestBody pipelineSpec: PipelineSpec<*,*>): Mono<SubmittedPipeline> =
      handleFeignErrors {
         pipelineApi.submitPipeline(pipelineSpec)
      }

   @GetMapping("/api/pipelines")
   fun getPipelines(): Mono<List<RunningPipelineSummary>> = handleFeignErrors { pipelineApi.getPipelines() }

   @GetMapping("/api/pipelines/{pipelineSpecId}")
   fun getPipeline(@PathVariable("pipelineSpecId") pipelineSpecId:String):Mono<RunningPipelineSummary>  = handleFeignErrors {
      pipelineApi.getPipeline(pipelineSpecId)
   }

   @DeleteMapping("/api/pipelines/{pipelineName}")
   fun removePipeline(@PathVariable("pipelineName") pipelineName: String) = handleFeignErrors { pipelineApi.deletePipeline(pipelineName) }

   fun <T> handleFeignErrors(method: () -> Mono<T>): Mono<T> {
      try {
         return method.invoke()
            .onErrorMap { e ->
               fun mapError(e: Throwable): Nothing {
                  when (e) {
                     is HystrixRuntimeException -> mapError(e.cause!!)
                     is FeignException -> {
                        val errorPayload = jacksonObjectMapper().readValue<Map<String,Any>>(e.contentUTF8())
                        val errorMessage = errorPayload["message"] as String? ?: e.message ?: e.contentUTF8()
                        throw BadRequestException(errorMessage)
                     }
                     else -> throw e
                  }
               }
               mapError(e)
            }
      } catch (e: FeignException.BadRequest) {
         throw BadRequestException(e.message ?: e.contentUTF8())
      }
   }
}
