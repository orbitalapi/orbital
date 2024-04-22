package com.orbitalhq.pipelines.jet.sink.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.spring.context.SpringAware
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.TypedInstanceContentProvider
import com.orbitalhq.pipelines.jet.api.transport.hazelcast.HazelcastTopicSinkSpec
import com.orbitalhq.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import org.springframework.stereotype.Component

/**
 * Emits records to a Hazelcast topic.
 * This is used for providing streaming HTTP results back on the Orbital server / HTTP gateways.
 * Consumers can request the messages from the topic, and emit over Websocket / SSE.
 *
 * Messages are not long-lived, and use the default TTL (250ms).
 */
@Component
class HazelcastTopicSinkBuilder : SingleMessagePipelineSinkBuilder<HazelcastTopicSinkSpec> {
   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean =
      pipelineTransportSpec is HazelcastTopicSinkSpec

   override fun build(
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: HazelcastTopicSinkSpec
   ): Sink<MessageContentProvider> {

      return SinkBuilder.sinkBuilder("hazelcast-topic") { context ->
         context.managedContext().initialize(
            HazelcastTopicSinkContext(context.hazelcastInstance(), pipelineTransportSpec)
         ) as HazelcastTopicSinkContext
      }.receiveFn { context: HazelcastTopicSinkContext, item: MessageContentProvider ->
         val rawItem = (item as TypedInstanceContentProvider).content.toRawObject()
         if (rawItem != null) {
            val topic = context.hazelcastInstance.getTopic<Any>(context.outputSpec.topicName)
            topic.publish(rawItem)
         }

      }
         .build()
   }

   override fun getRequiredType(pipelineTransportSpec: HazelcastTopicSinkSpec, schema: Schema): QualifiedName? = null
}


@SpringAware
class HazelcastTopicSinkContext(
   val hazelcastInstance: HazelcastInstance,
   val outputSpec: HazelcastTopicSinkSpec,
)
