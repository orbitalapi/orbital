package io.vyne.pipelines.jet

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.pipelines")
data class PipelineConfig(
   val pipelinePath: Path? = null
)
