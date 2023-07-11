package io.vyne.cockpit.core.pipelines

import io.vyne.UriSafePackageIdentifier
import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.PipelineStatus
import io.vyne.pipelines.jet.api.RunningPipelineSummary
import io.vyne.pipelines.jet.api.SubmittedPipeline
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.security.VynePrivileges
import io.vyne.spring.http.handleFeignErrors
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger { }

/**
 * Query server facade which forwards requests onto the pipeline orchestrator
 */
@RestController
class PipelineServiceFacade(private val pipelineApi: PipelineApi) {

   @PreAuthorize("hasAuthority('${VynePrivileges.EditPipelines}')")
   @PostMapping("/api/pipelines/{packageIdentifier}")
   fun submitPipeline(
      @PathVariable("packageIdentifier") packageUri: UriSafePackageIdentifier,
      @RequestBody pipelineSpec: PipelineSpec<*, *>
   ): Mono<SubmittedPipeline> =
      handleFeignErrors {
         pipelineApi.submitPipeline(packageUri, pipelineSpec)
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


