package io.vyne.pipelines.orchestrator.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.pipelines.orchestrator.PipelineReference
import io.vyne.pipelines.orchestrator.PipelineRunnerInstance
import io.vyne.pipelines.orchestrator.PipelineStateSnapshot
import io.vyne.pipelines.orchestrator.PipelinesManager
import io.vyne.pipelines.orchestrator.configuration.FileBasedPipelineConfigurations
import io.vyne.pipelines.orchestrator.configuration.PipelineConfigurationProperties
import io.vyne.pipelines.orchestrator.configuration.PipelineConfigurationProperty
import io.vyne.utils.log
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import java.io.InputStreamReader

@Service
class PipelinesService(val pipelineManager: PipelinesManager,
                       val pipelineDeserialiser: PipelineDeserialiser,
                       val objectMapper: ObjectMapper,
                       val pipelineConfigurationProperties: PipelineConfigurationProperties?) : InitializingBean {

   override fun afterPropertiesSet() {
      pipelineConfigurationProperties?.let { pipelineConfigurationProperties ->
         processDefinitionsFromConfigurationProperty(pipelineConfigurationProperties.definition.map { pipelineConfigurationPropertyToJsonString(it) })
         processDefinitionsFromFile(pipelineConfigurationProperties.definitions)
      }
   }

   fun initialisePipeline(pipelineDescription: String): PipelineStateSnapshot {
      val pipeline = pipelineDeserialiser.deserialise(pipelineDescription)
      return pipelineManager.addPipeline(PipelineReference(pipeline.name, pipelineDescription))
   }

   fun runners() = pipelineManager.runnerInstances.map { PipelineRunnerInstance(it.instanceId, it.uri.toString()) }

   fun pipelines() = pipelineManager.pipelines.map { it.value }

   private fun pipelineConfigurationPropertyToJsonString(pipelineConfigurationProperty: PipelineConfigurationProperty): String {
      return objectMapper.writeValueAsString(mapOf(
         "name" to pipelineConfigurationProperty.name,
         "input" to io(pipelineConfigurationProperty.input),
         "output" to io(pipelineConfigurationProperty.output)
      ))
   }

   private fun io(io: Map<String, Any>): Map<String, Any> {
      val transport = io["transport"] as Map<String, Any>?
      val inputProps = transport?.let {
         (it["props"] as Map<String, Any>?)
      }
      val flattenedInputProps = inputProps?.map { (key, value) ->
         flatten(value, key)
      }

      val result = flattenedInputProps?.let {
         val mutableTransportMap = transport.toMutableMap()
         mutableTransportMap["props"] = it.toMap()
         val ioMutableMap = io.toMutableMap()
         ioMutableMap["transport"] = mutableTransportMap
         ioMutableMap
      }
     return result?.toMap() ?: io
   }

   private fun flatten(value: Any, prefix: String): Pair<String, Any> {
      return if (value is Map<*, *>) {
         flatten(value.values.first()!!, "$prefix.${value.keys.first()}")
      } else {
         Pair(prefix, value)
      }

   }

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
            val node = objectMapper.readTree(fileBasedPipelineDefs.location.toFile().inputStream())
            if (node.isArray) {
               node.forEach { pipelineDefinition ->
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
