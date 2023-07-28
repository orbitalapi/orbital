package io.vyne.pipelines.jet

import io.vyne.PackageIdentifier
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

//@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.pipelines")
data class PipelineConfig(
   val pipelinePath: Path? = null
) {
   companion object {
      /**
       * Package id for the config we load at the system level (not for loading from config
       * sitting inside packages)
       */
      val PACKAGE_IDENTIFIER = PackageIdentifier.fromId("com.orbitalhq.config/pipelines/1.0.0")
   }
}
