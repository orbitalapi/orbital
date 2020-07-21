package io.vyne.pipelines.orchestrator.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.pipelines.orchestrator.PipelineReference
import io.vyne.pipelines.orchestrator.PipelineRunnerInstance
import io.vyne.pipelines.orchestrator.PipelineStateSnapshot
import io.vyne.pipelines.orchestrator.PipelinesManager
import io.vyne.pipelines.orchestrator.configuration.FileBasedPipelineConfigurations
import io.vyne.pipelines.orchestrator.configuration.PipelineConfigurationProperties
import io.vyne.utils.log
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStreamReader

@Service
class PipelinesService(val pipelineManager: PipelinesManager,
                       val pipelineDeserialiser: PipelineDeserialiser,
                       val objectMapper: ObjectMapper,
                       val pipelineConfigurationProperties: PipelineConfigurationProperties?): InitializingBean {

   override fun afterPropertiesSet() {
      pipelineConfigurationProperties?.let {
         processDefinitionsFromConfigurationProperty(it.definition)
         processDefinitionsFromFile(it.definitions)
      }
   }

   fun initialisePipeline(pipelineDescription: String): PipelineStateSnapshot {
      val pipeline = pipelineDeserialiser.deserialise(pipelineDescription)
      return  pipelineManager.addPipeline(PipelineReference(pipeline.name, pipelineDescription))
   }

   fun runners() = pipelineManager.runnerInstances.map { PipelineRunnerInstance(it.instanceId, it.uri.toString()) }

   fun pipelines() = pipelineManager.pipelines.map { it.value }

   private fun processDefinitionsFromConfigurationProperty(pipelineDefinitions: List<String>) {
      pipelineDefinitions.forEach { pipelineJsonDefinition ->
         log().info("Initialising the pipeline => $pipelineJsonDefinition")
         try {
            initialisePipeline(pipelineJsonDefinition)
         } catch (e: Exception) {
            log().error("Error in initialising the pipeline definition read from configuration", e)
         }
      }
   }

   private fun processDefinitionsFromFile(definitions: FileBasedPipelineConfigurations?) {
      definitions?.let { fileBasedPipelineDefs ->
         log().info("Reading pipeline definitions from ${fileBasedPipelineDefs.location}")
         try {
            val node =  objectMapper.readTree(InputStreamReader(fileBasedPipelineDefs.location.inputStream))
            if (node.isArray) {
               node.forEach {
                  pipelineDefinition ->
                  try {
                     val pipelineJsonStringDefinition = objectMapper.writeValueAsString(pipelineDefinition)
                     log().info("Initialising the pipeline => $pipelineJsonStringDefinition")
                     initialisePipeline(pipelineJsonStringDefinition)
                  } catch (e: Exception) {
                     log().error("Error in initialising the pipeline definition read from configuration", e)
                  }
               }
            } else {
               log().info("${fileBasedPipelineDefs.location} should contain pipeline definition(s) in a json array")
            }
         } catch (e: Exception) {
            log().error("Error in initialising the pipeline definition read from file", e)
         }
      }
   }
}
