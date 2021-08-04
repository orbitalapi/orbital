package io.vyne.pipelines.runner.transport.http

import io.vyne.VersionedTypeReference
import io.vyne.annotations.http.HttpOperations
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.PipelineTransportType
import io.vyne.pipelines.runner.transport.PipelineTransportSpecId

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
   val payloadType: VersionedTypeReference
) : PipelineTransportSpec {
   companion object {
      val specId = PipelineTransportSpecId(
         HttpListenerTransport.TYPE,
         PipelineDirection.INPUT,
         HttpListenerTransportSpec::class.java
      )
   }

   override val type: PipelineTransportType = HttpListenerTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val props: Map<String, Any> = emptyMap()
   override val description: String = "Schema operation  $method requests at $path"
}

