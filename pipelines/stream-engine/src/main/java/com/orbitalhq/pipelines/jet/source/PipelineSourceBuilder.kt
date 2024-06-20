package com.orbitalhq.pipelines.jet.source

import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.StreamSource
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type

interface PipelineSourceBuilder<I : PipelineTransportSpec> {
   fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean
   fun build(pipelineSpec: PipelineSpec<I, *>, inputType: Type?): StreamSource<MessageContentProvider>? {
      return null
   }

   fun buildBatch(pipelineSpec: PipelineSpec<I, *>, inputType: Type?): BatchSource<MessageContentProvider>? {
      return null
   }

   /**
    * @return The emitted type or null when there's no need to do any transformations for the result before the output stage
    */
   fun getEmittedType(pipelineSpec: PipelineSpec<I, *>, schema: Schema): QualifiedName?

   val sourceType: PipelineSourceType
      get() = PipelineSourceType.Stream
}

enum class PipelineSourceType {
   Batch,
   Stream
}
