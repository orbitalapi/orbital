package io.vyne.pipelines.jet.source.http.listener

import com.hazelcast.jet.pipeline.StreamSource
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.api.transport.http.HttpListenerTransportSpec
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema

class HttpListenerSource : PipelineSourceBuilder<HttpListenerTransportSpec> {
   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean = pipelineSpec.input is HttpListenerTransportSpec

   override fun build(pipelineSpec: PipelineSpec<HttpListenerTransportSpec, *>): StreamSource<MessageContentProvider> {
      TODO("Not yet implemented")
   }

   override fun getEmittedType(
       pipelineSpec: PipelineSpec<HttpListenerTransportSpec, *>,
       schema: Schema
   ): QualifiedName {
      return pipelineSpec.input.payloadType.typeName
   }
}
