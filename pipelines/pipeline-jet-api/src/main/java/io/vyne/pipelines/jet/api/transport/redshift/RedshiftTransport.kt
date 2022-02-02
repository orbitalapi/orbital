package io.vyne.pipelines.jet.api.transport.redshift

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType

object RedshiftTransport {
   const val TYPE: PipelineTransportType = "redshift"
   val INPUT = RedshiftTransportInputSpec.specId
   val OUTPUT = RedshiftTransportOutputSpec.specId
}

open class RedshiftTransportInputSpec(
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
         PipelineTransportSpecId(RedshiftTransport.TYPE, PipelineDirection.INPUT, RedshiftTransportInputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(targetTypeName)
      }

   override val description: String = "Redshift props: $props"
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = RedshiftTransport.TYPE
}


data class RedshiftTransportOutputSpec(
   val connection: String,
   final override val props: Map<String, Any>,
   val targetTypeName: String
) : PipelineTransportSpec {
   constructor(
      connection: String,
      props: Map<String, Any>,
      targetType: VersionedTypeReference
   ) : this(connection, props, targetType.toString())

   companion object {
      val specId =
         PipelineTransportSpecId(RedshiftTransport.TYPE, PipelineDirection.OUTPUT, RedshiftTransportOutputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(this.targetTypeName)
      }
   override val description: String = "Redshift connection $connection"

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = RedshiftTransport.TYPE
}