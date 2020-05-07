package io.vyne.pipelines.runner.transport.kafka

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.PipelineTransportType
import io.vyne.pipelines.runner.transport.PipelineTransportSpecId

object KafkaTransport {
   const val TYPE: PipelineTransportType = "kafka"
   val INPUT = KafkaTransportInputSpec.specId
   val OUTPUT = KafkaTransportOutputSpec.specId
}

data class KafkaTransportInputSpec(
   val topic: String,
   override val targetType: VersionedTypeReference,
   val props: Map<String, Any>
) : PipelineTransportSpec {
   companion object {
      val specId = PipelineTransportSpecId(KafkaTransport.TYPE, PipelineDirection.INPUT, KafkaTransportInputSpec::class.java)
   }

   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = KafkaTransport.TYPE
}


data class KafkaTransportOutputSpec(
   val topic: String,
   val props: Map<String, Any>,
   override val targetType: VersionedTypeReference
) : PipelineTransportSpec {
   companion object {
      val specId = PipelineTransportSpecId(KafkaTransport.TYPE, PipelineDirection.OUTPUT, KafkaTransportOutputSpec::class.java)
   }

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = KafkaTransport.TYPE
}
