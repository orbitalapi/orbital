package com.orbitalhq.pipelines.jet

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VyneTypes
import java.nio.file.Path

//@ConstructorBinding
data class PipelineConfig(
   val pipelinePath: Path? = null
) {
   companion object {
      /**
       * Package id for the config we load at the system level (not for loading from config
       * sitting inside packages)
       */
      val PACKAGE_IDENTIFIER = PackageIdentifier.fromId("${VyneTypes.NAMESPACE}.config/pipelines/1.0.0")
   }
}
