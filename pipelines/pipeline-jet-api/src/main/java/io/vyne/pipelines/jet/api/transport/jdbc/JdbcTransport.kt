package io.vyne.pipelines.jet.api.transport.jdbc

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import io.vyne.pipelines.jet.api.transport.WindowingPipelineTransportSpec

object JdbcTransport {
   const val TYPE: PipelineTransportType = "jdbc"
   val INPUT = JdbcTransportInputSpec.specId
   val OUTPUT = JdbcTransportOutputSpec.specId
}

open class JdbcTransportInputSpec(
   val topic: String,
   val targetTypeName: String,
   final override val props: Map<String, Any> = emptyMap()
) : PipelineTransportSpec {
   constructor(
      topic: String,
      targetType: VersionedTypeReference,
      props: Map<String, Any>
   ) : this(topic, targetType.toString(), props)

   companion object {
      val specId =
         PipelineTransportSpecId(JdbcTransport.TYPE, PipelineDirection.INPUT, JdbcTransportInputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(targetTypeName)
      }

   override val description: String = "Jdbc props: $props"
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = JdbcTransport.TYPE
}


data class JdbcTransportOutputSpec(
   val connection: String,
   override val props: Map<String, Any> = emptyMap(),
   val targetTypeName: String
) : WindowingPipelineTransportSpec {
   constructor(
      connection: String,
      props: Map<String, Any>,
      targetType: VersionedTypeReference
   ) : this(connection, props, targetType.toString())

   companion object {
      val specId =
         PipelineTransportSpecId(JdbcTransport.TYPE, PipelineDirection.OUTPUT, JdbcTransportOutputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(this.targetTypeName)
      }
   override val description: String = "Jdbc connection $connection"

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = JdbcTransport.TYPE
   override val windowDurationMs: Long = 500
}
