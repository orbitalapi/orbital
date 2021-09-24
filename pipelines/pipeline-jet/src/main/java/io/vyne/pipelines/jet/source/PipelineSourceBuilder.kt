package io.vyne.pipelines.jet.source

import com.hazelcast.jet.pipeline.StreamSource
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema

interface PipelineSourceBuilder<I : PipelineTransportSpec> {
   fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean
   fun build(pipelineSpec: PipelineSpec<I,*>): StreamSource<MessageContentProvider>
   fun getEmittedType(pipelineSpec: PipelineSpec<I,*>, schema:Schema): QualifiedName
}
