package io.vyne.pipelines.runner.transport.cask

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.PipelineTransportType
import io.vyne.pipelines.runner.transport.PipelineTransportSpecId

object CaskTransport {
   const val TYPE: PipelineTransportType = "cask"
   val OUTPUT = CaskTransportOutputSpec.specId
}

data class CaskTransportOutputSpec(
   override val props: Map<String, String> = mapOf(),
   override val targetType: VersionedTypeReference
) : PipelineTransportSpec {

   override val description: String = "Cask for $targetType"

   companion object {
      val specId = PipelineTransportSpecId(CaskTransport.TYPE, PipelineDirection.OUTPUT, CaskTransportOutputSpec::class.java)
   }

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = CaskTransport.TYPE
}
