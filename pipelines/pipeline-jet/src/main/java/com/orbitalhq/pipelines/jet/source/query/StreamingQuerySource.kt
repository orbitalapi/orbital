package com.orbitalhq.pipelines.jet.source.query

import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.SourceBuilder.SourceBuffer
import com.hazelcast.jet.pipeline.StreamSource
import com.orbitalhq.embedded.EmbeddedVyneClient
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.TypedInstanceContentProvider
import com.orbitalhq.pipelines.jet.api.transport.query.StreamingQueryInputSpec
import com.orbitalhq.pipelines.jet.source.PipelineSourceBuilder
import com.orbitalhq.pipelines.jet.source.PipelineSourceType
import com.orbitalhq.query
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.coroutineContext

class StreamingQuerySourceBuilder() :
   PipelineSourceBuilder<StreamingQueryInputSpec> {
   override val sourceType: PipelineSourceType
      get() = PipelineSourceType.Stream

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean = pipelineSpec.input is StreamingQueryInputSpec


   override fun getEmittedType(pipelineSpec: PipelineSpec<StreamingQueryInputSpec, *>, schema: Schema): QualifiedName? =
      null

   override fun build(
      pipelineSpec: PipelineSpec<StreamingQueryInputSpec, *>,
      inputType: Type?
   ): StreamSource<MessageContentProvider>? {
      return SourceBuilder.stream(pipelineSpec.name) { context ->
         QueryBufferingPipelineContext(context.logger(), pipelineSpec, context.jobId(), QueryBufferingPipelineContext.BufferMode.Stream)
      }.fillBufferFn { pipelineContext, sourceBuffer: SourceBuffer<MessageContentProvider> ->
         pipelineContext.fillBuffer(sourceBuffer)
      }
         .build()
   }
}


private class FluxBuffer<T>(flux: Flux<T>) {
   val queue = LinkedBlockingQueue<T>()

   init {
      flux.subscribe { queue.offer(it) }
   }
}
