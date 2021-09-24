package io.vyne.queryService.pipelines

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.pipelines")
data class PipelineConfig(
   val kibanaUrl: String? = null,
   val logsIndex: String? = null
) {

}
