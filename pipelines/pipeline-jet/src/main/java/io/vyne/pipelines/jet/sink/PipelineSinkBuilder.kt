package io.vyne.pipelines.jet.sink

import com.hazelcast.jet.pipeline.Sink
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema

interface PipelineSinkBuilder<O : PipelineTransportSpec> {
   fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean
   fun getRequiredType(pipelineSpec: PipelineSpec<*,O>, schema:Schema): QualifiedName
   fun build(pipelineSpec: PipelineSpec<*,O>): Sink<MessageContentProvider>

}
