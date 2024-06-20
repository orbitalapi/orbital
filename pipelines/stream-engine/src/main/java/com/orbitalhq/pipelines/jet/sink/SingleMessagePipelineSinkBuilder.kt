package com.orbitalhq.pipelines.jet.sink

import com.hazelcast.jet.datamodel.WindowResult
import com.hazelcast.jet.pipeline.Sink
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.WindowingPipelineTransportSpec
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema

interface PipelineSinkBuilder<O : PipelineTransportSpec, TSinkType> {
   fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean

   /**
    * Defines the target type.
    * Sinks can return null here to indicate that they'll inherit the type upstream,
    * from the transformation stage.
    *
    * This allows transformations to emit anonymous types.
    *
    * If null is returned here, then there must be a transformation stage defined.
    */
   fun getRequiredType(pipelineTransportSpec: O, schema: Schema): QualifiedName?
   fun build(pipelineId: String, pipelineName: String, pipelineTransportSpec: O): Sink<TSinkType>
}

/**
 * A Pipeline Sink builder for sinks which operate on a single message at a time
 */
interface SingleMessagePipelineSinkBuilder<O : PipelineTransportSpec> : PipelineSinkBuilder<O, MessageContentProvider>

/**
 * A Pipeline Sink Builder for sinks that accept a batched window of messages
 */
interface WindowingPipelineSinkBuilder<O : WindowingPipelineTransportSpec> :
   PipelineSinkBuilder<O, WindowResult<List<MessageContentProvider>>>
