package io.vyne.cockpit.core.pipelines

import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.PipelineStatus
import io.vyne.pipelines.jet.api.RunningPipelineSummary
import io.vyne.pipelines.jet.api.SubmittedPipeline
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.security.VynePrivileges
import io.vyne.spring.http.handleFeignErrors
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger { }

/**
 * Query server facade which forwards requests onto the pipeline orchestrator
 */
@RestController
class PipelineServiceFacade(private val pipelineApi: PipelineApi) {

   @PreAuthorize("hasAuthority('${VynePrivileges.EditPipelines}')")
   @PostMapping("/api/pipelines")
   fun submitPipeline(@RequestBody pipelineSpec: PipelineSpec<*, *>): Mono<SubmittedPipeline> =
      handleFeignErrors {
         pipelineApi.submitPipeline(pipelineSpec)
      }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewPipelines}')")
   @GetMapping("/api/pipelines")
   fun getPipelines(): Mono<List<RunningPipelineSummary>> = handleFeignErrors { pipelineApi.getPipelines() }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewPipelines}')")
   @GetMapping("/api/pipelines/{pipelineSpecId}")
   fun getPipeline(@PathVariable("pipelineSpecId") pipelineSpecId: String): Mono<RunningPipelineSummary> =
      handleFeignErrors {
         pipelineApi.getPipeline(pipelineSpecId)
      }


   @PreAuthorize("hasAuthority('${VynePrivileges.EditPipelines}')")
   @DeleteMapping("/api/pipelines/{pipelineSpecId}")
   fun removePipeline(@PathVariable("pipelineSpecId") pipelineSpecId: String): Mono<PipelineStatus> =
      handleFeignErrors {
         logger.info { "Deleting pipeline $pipelineSpecId" }
         pipelineApi.deletePipeline(pipelineSpecId)
      }
}


