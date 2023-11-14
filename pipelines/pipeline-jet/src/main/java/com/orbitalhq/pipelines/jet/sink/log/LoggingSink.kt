package com.orbitalhq.pipelines.jet.sink.log

import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.logging.ILogger
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.TypedInstanceContentProvider
import com.orbitalhq.pipelines.jet.api.transport.log.LoggingOutputSpec
import com.orbitalhq.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import org.springframework.stereotype.Component


@Component
class LoggingSinkBuilder : SingleMessagePipelineSinkBuilder<LoggingOutputSpec> {

   companion object {
      val captured = mutableListOf<Any>()
      fun resetCaptured() {
         captured.clear()
      }
   }

   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean =
      pipelineTransportSpec is LoggingOutputSpec

   override fun build(
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: LoggingOutputSpec
   ): Sink<MessageContentProvider> {
      return SinkBuilder.sinkBuilder("logging-sink") { a -> LoggingSinkContext(pipelineTransportSpec, a.logger()) }
         .receiveFn { context, item: MessageContentProvider ->
            val rawItem = (item as TypedInstanceContentProvider).content.toRawObject()
            context.logger.info("[${context.spec.logger}] Item Recieved: $rawItem")
            if (context.spec.captureForTest) {
               captured.add(item)
            }
         }
         .build()
   }

   override fun getRequiredType(pipelineTransportSpec: LoggingOutputSpec, schema: Schema): QualifiedName? = null
}

data class LoggingSinkContext(val spec: LoggingOutputSpec, val logger: ILogger)
