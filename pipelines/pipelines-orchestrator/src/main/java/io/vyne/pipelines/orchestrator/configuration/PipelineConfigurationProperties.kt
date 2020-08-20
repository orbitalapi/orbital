package io.vyne.pipelines.orchestrator.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.Resource

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.pipeline")
data class PipelineConfigurationProperties(val definition: List<PipelineConfigurationProperty> = listOf(), val definitions: FileBasedPipelineConfigurations? = null)

data class FileBasedPipelineConfigurations(val location: Resource)

data class PipelineConfigurationProperty(val name: String, val input: Map<String, Any>, val output: Map<String, Any>)

