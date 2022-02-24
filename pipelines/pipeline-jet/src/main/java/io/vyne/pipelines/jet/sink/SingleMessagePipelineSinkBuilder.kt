package io.vyne.pipelines.jet.sink

import com.hazelcast.jet.datamodel.WindowResult
import com.hazelcast.jet.pipeline.Sink
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.WindowingPipelineTransportSpec
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema

interface PipelineSinkBuilder<O : PipelineTransportSpec, TSinkType> {
   fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean
   fun getRequiredType(pipelineSpec: PipelineSpec<*, O>, schema: Schema): QualifiedName
   fun build(pipelineSpec: PipelineSpec<*, O>): Sink<TSinkType>
}

/**
 * A Pipeline SInk builder for sinks which operates on a single message at a time
 */
interface SingleMessagePipelineSinkBuilder<O : PipelineTransportSpec> : PipelineSinkBuilder<O, MessageContentProvider>

/**
 * A Pipeline Sink Builder for sinks that accept a batched window of messages
 */
interface WindowingPipelineSinkBuilder<O : WindowingPipelineTransportSpec> :
   PipelineSinkBuilder<O, WindowResult<List<MessageContentProvider>>> {
}