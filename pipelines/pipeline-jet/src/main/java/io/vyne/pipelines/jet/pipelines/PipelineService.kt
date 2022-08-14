package io.vyne.pipelines.jet.pipelines

import io.vyne.pipelines.jet.api.JobStatus
import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.PipelineStatus
import io.vyne.pipelines.jet.api.RunningPipelineSummary
import io.vyne.pipelines.jet.api.SubmittedPipeline
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.schema.consumer.SchemaChangedEventProvider
import io.vyne.schemas.taxi.TaxiSchema
import mu.KotlinLogging
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import javax.annotation.PostConstruct

@RestController
class PipelineService(
   private val pipelineManager: PipelineManager,
   private val pipelineRepository: PipelineRepository,
   private val schemaStore: SchemaChangedEventProvider,
) : PipelineApi {

   private val logger = KotlinLogging.logger {}

   @PostConstruct
   fun loadAndSubmitExistingPipelines() {
      var remainingPipelines = pipelineRepository.loadPipelines()
      Flux.from(schemaStore.schemaChanged)
         .subscribe { schemaChangedEvent ->
            remainingPipelines = checkReceivedTypesForPipelinesAndStartAppropriateOnes(
               remainingPipelines,
               schemaChangedEvent.newSchemaSet.schema.asTaxiSchema()
            )
         }
   }

   private fun checkReceivedTypesForPipelinesAndStartAppropriateOnes(
      pipelinesToBeSubmitted: List<PipelineSpec<*, *>>,
      schema: TaxiSchema
   ): List<PipelineSpec<*, *>> {
      return pipelinesToBeSubmitted.filter { pipelineSpec ->
         logger.info("Trying to submit the loaded pipeline ${pipelineSpec.name}.")
         val typesMissingForInput = pipelineSpec.input.requiredSchemaTypes.filter { !schema.hasType(it) }
         val typesMissingForOutput = pipelineSpec.output.requiredSchemaTypes.filter { !schema.hasType(it) }
         val typesMissing = typesMissingForInput + typesMissingForOutput
         if (typesMissing.isNotEmpty()) {
            logger.error(
               "The following types are missing for the pipeline ${pipelineSpec.name}: ${
                  typesMissing.joinToString(
                     ", "
                  )
               }. Pipeline will be started as soon as they become available in the schema."
            )
            return@filter true
         }
         try {
            pipelineManager.startPipeline(pipelineSpec)
            false
         } catch (e: Exception) {
            logger.error(e) { "Loaded pipeline ${pipelineSpec.name} (${pipelineSpec.id}) failed to start. Retrying on the next schema update event." }
            true
         }
      }
   }

   @PostMapping("/api/pipelines")
   override fun submitPipeline(@RequestBody pipelineSpec: PipelineSpec<*, *>): Mono<SubmittedPipeline> {
      logger.info { "Received new pipelineSpec: \n${pipelineSpec}" }
      pipelineRepository.save(pipelineSpec)
      val (submittedPipeline) = pipelineManager.startPipeline(pipelineSpec)

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

