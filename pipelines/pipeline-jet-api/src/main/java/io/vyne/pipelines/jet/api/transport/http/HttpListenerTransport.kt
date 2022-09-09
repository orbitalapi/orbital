package io.vyne.pipelines.jet.api.transport.http

import io.vyne.VersionedTypeReference
import io.vyne.annotations.http.HttpOperations
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType

/**
 * Transport that exposes an HTTP endpoint, which can receive trigger events
 */
object HttpListenerTransport {
   const val TYPE: PipelineTransportType = "httpListener"
   val INPUT = HttpListenerTransportSpec.specId
}

data class HttpListenerTransportSpec(
   val path: String,
   val method: HttpOperations.HttpMethod,
   val payloadTypeName: String
) : PipelineTransportSpec {
   constructor(path: String,
               method: HttpOperations.HttpMethod,
               payloadType: VersionedTypeReference) : this(path, method, payloadType.toString())
   companion object {
      val specId = PipelineTransportSpecId(
         HttpListenerTransport.TYPE,
         PipelineDirection.INPUT,
         HttpListenerTransportSpec::class.java
      )
   }

   override val type: PipelineTransportType = HttpListenerTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val description: String = "Schema operation  $method requests at $path"

   val payloadType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(payloadTypeName)
      }
}

