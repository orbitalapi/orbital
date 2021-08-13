package io.vyne.pipelines.runner.transport.kafka

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.PipelineTransportType
import io.vyne.pipelines.runner.transport.PipelineTransportSpecId
import io.vyne.utils.ImmutableEquality

object KafkaTransport {
   const val TYPE: PipelineTransportType = "kafka"
   val INPUT = KafkaTransportInputSpec.specId
   val OUTPUT = KafkaTransportOutputSpec.specId
}

open class KafkaTransportInputSpec(
   val topic: String,
   val targetType: VersionedTypeReference,
   final override val props: Map<String, Any>
) : PipelineTransportSpec {
   companion object {
      val specId =
         PipelineTransportSpecId(KafkaTransport.TYPE, PipelineDirection.INPUT, KafkaTransportInputSpec::class.java)
   }

   private val equality = ImmutableEquality(
      this, KafkaTransportInputSpec::topic,
      KafkaTransportInputSpec::targetType,
      KafkaTransportInputSpec::props
   )


   override fun equals(other: Any?) = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()

   override val description: String = "Kafka topic: $topic, props: $props"
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = KafkaTransport.TYPE
}


data class KafkaTransportOutputSpec(
   val topic: String,
   final override val props: Map<String, Any>,
   val targetType: VersionedTypeReference
) : PipelineTransportSpec {
   companion object {
      val specId =
         PipelineTransportSpecId(KafkaTransport.TYPE, PipelineDirection.OUTPUT, KafkaTransportOutputSpec::class.java)
   }

   override val description: String = "Kafka topic ${props["topic"] ?: "Undefined"}"

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = KafkaTransport.TYPE
}
