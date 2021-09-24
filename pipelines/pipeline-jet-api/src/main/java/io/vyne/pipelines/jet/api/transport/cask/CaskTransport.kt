package io.vyne.pipelines.jet.api.transport.cask

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType

object CaskTransport {
   const val TYPE: PipelineTransportType = "cask"
   val OUTPUT = CaskTransportOutputSpec.specId
}

data class CaskTransportOutputSpec(
   override val props: Map<String, String> = mapOf(),
   val targetType: VersionedTypeReference
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
