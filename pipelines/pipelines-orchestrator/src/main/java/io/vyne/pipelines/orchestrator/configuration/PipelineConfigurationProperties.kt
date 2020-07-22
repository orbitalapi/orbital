package io.vyne.pipelines.orchestrator.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.pipeline")
data class PipelineConfigurationProperties(val definition: List<String> = listOf(), val definitions: FileBasedPipelineConfigurations? = null)

data class FileBasedPipelineConfigurations(val location: Resource)
