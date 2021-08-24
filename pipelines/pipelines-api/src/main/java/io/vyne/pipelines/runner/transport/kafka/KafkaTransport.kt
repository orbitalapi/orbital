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

open class KafkaTransportInputSpec(
   val topic: String,
   val targetTypeName: String,
   final override val props: Map<String, Any>
) : PipelineTransportSpec {
   constructor(
      topic: String,
      targetType: VersionedTypeReference,
      props: Map<String, Any>
   ) : this(topic, targetType.toString(), props)

   companion object {
      val specId =
         PipelineTransportSpecId(KafkaTransport.TYPE, PipelineDirection.INPUT, KafkaTransportInputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(targetTypeName)
      }

   override val description: String = "Kafka topic: $topic, props: $props"
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = KafkaTransport.TYPE
}


data class KafkaTransportOutputSpec(
   val topic: String,
   final override val props: Map<String, Any>,
   val targetTypeName: String
) : PipelineTransportSpec {
   constructor(
      topic: String,
      props: Map<String, Any>,
      targetType: VersionedTypeReference
   ) : this(topic, props, targetType.toString())

   companion object {
      val specId =
         PipelineTransportSpecId(KafkaTransport.TYPE, PipelineDirection.OUTPUT, KafkaTransportOutputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(this.targetTypeName)
      }
   override val description: String = "Kafka topic $topic"

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = KafkaTransport.TYPE
}
