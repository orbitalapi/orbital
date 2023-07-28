package io.vyne.pipelines.jet.pipelines

import io.vyne.PackageIdentifier
import io.vyne.UriSafePackageIdentifier
import io.vyne.pipelines.jet.api.*
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.taxi.TaxiSchema
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
class PipelineService(
   private val pipelineManager: PipelineManager,
   private val pipelineRepository: PipelineConfigRepository,
   private val schemaStore: SchemaStore,
) : PipelineApi {

   private val logger = KotlinLogging.logger {}

   init {
//      Flux.from(schemaStore.schemaChanged).subscribe { schemaChangedEvent ->
//         val pipelines = pipelineRepository.loadPipelines()
//         val schema = schemaChangedEvent.newSchemaSet.schema.asTaxiSchema()
//         checkReceivedTypesForPipelinesAndStartAppropriateOnes(
//            pipelines,
//            schema
//         )
//      }
//
      pipelineRepository.configUpdated.subscribe {
         logger.info { "Pipeline sources have changed, resubmitting pipelines" }
         loadAndSubmitPipelines()
      }

      logger.info { "Triggering load of pipelines on startup" }
      loadAndSubmitPipelines()

   }

   private fun loadAndSubmitPipelines() {
      val pipelines = pipelineRepository.loadPipelines()
      val schema = schemaStore.schema().asTaxiSchema()
      checkReceivedTypesForPipelinesAndStartAppropriateOnes(
         pipelines,
         schema
      )
   }

   @PostConstruct
   fun loadAndSubmitExistingPipelines() {

//      Flux.from(schemaStore.schemaChanged)
//         .subscribe { schemaChangedEvent ->
//            val schema = schemaChangedEvent.newSchemaSet.schema.asTaxiSchema()
//            remainingPipelines = checkReceivedTypesForPipelinesAndStartAppropriateOnes(
//               remainingPipelines,
//               schema
//            )
//
//             Load the pipelines from the schema
//            val pipelineSourcePackages = schema.additionalSources["@orbital/pipelines"] ?: emptyList()
//            val sources = pipelineSourcePackages.flatMap { it.sources }
//            val pipelines = pipelineRepository.loadPipelines(sources)
//            logger.info { "The schema contains ${sources.size} files containing pipeline definitions, which generated ${pipelines.size} pipelines" }
//            checkReceivedTypesForPipelinesAndStartAppropriateOnes(pipelines, schema)
//         }
   }

   private fun checkReceivedTypesForPipelinesAndStartAppropriateOnes(
      pipelinesToBeSubmitted: List<PipelineSpec<*, *>>,
      schema: TaxiSchema
   ): List<PipelineSpec<*, *>> {
      return pipelinesToBeSubmitted.filter { pipelineSpec ->
         val typesMissingForInput = pipelineSpec.input.requiredSchemaTypes.filter { !schema.hasType(it) }
         val typesMissingForOutputs =
            pipelineSpec.outputs.flatMap { output -> output.requiredSchemaTypes.filter { !schema.hasType(it) } }
         val typesMissing = typesMissingForInput + typesMissingForOutputs
         if (typesMissing.isNotEmpty()) {
            logger.error(
               "The following types are missing for the pipeline \"${pipelineSpec.name}\": ${
                  typesMissing.joinToString(
                     ", "
                  )
               }. Pipeline will be started as soon as they become available in the schema."
            )
            return@filter true
         }
         try {
            logger.info("Pipeline \"${pipelineSpec.name}\" has all the required types available.")
            pipelineManager.startPipeline(pipelineSpec)
            false
         } catch (e: Exception) {
            logger.error(e) { "Loaded pipeline \"${pipelineSpec.name}\" failed to start. Retrying on the next schema update event." }
            true
         }
      }
   }

   @PostMapping("/api/pipelines/scheduled")
   fun triggerScheduledPipeline(@RequestBody triggerScheduledPipelineRequest: TriggerScheduledPipelineRequest): Mono<TriggerScheduledPipelineResponse> {
      logger.info { "Received request to trigger a schedule pipeline manually => $triggerScheduledPipelineRequest" }
      return Mono.just(
         TriggerScheduledPipelineResponse(
            pipelineManager.triggerScheduledPipeline(
               triggerScheduledPipelineRequest.pipelineSpecId
            )
         )
      )
   }

   @PostMapping("/api/pipelines/{packageIdentifier}")
   override fun submitPipeline(
      @PathVariable("packageIdentifier") packageUri: UriSafePackageIdentifier,
      @RequestBody pipelineSpec: PipelineSpec<*, *>
   ): Mono<SubmittedPipeline> {
      val packageIdentifier = PackageIdentifier.fromUriSafeId(packageUri)
      logger.info { "Received new pipelineSpec: \n${pipelineSpec}" }
      pipelineRepository.save(packageIdentifier, pipelineSpec)
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
      val status = pipelineManager.terminatePipeline(pipelineSpecId)
      TODO("Deleting pipelines not supported whilst we migrate to using schema loaders")
//      if (status.status != JobStatus.RUNNING && status.status != JobStatus.SCHEDULED) {
//         val pipeline = pipelineManager.getPipeline(pipelineSpecId)
//         pipelineRepository.deletePipeline(pipeline.pipeline!!.spec)
//      }
//      return Mono.just(status)
   }

}

data class TriggerScheduledPipelineRequest(val pipelineSpecId: String)
data class TriggerScheduledPipelineResponse(val success: Boolean)
