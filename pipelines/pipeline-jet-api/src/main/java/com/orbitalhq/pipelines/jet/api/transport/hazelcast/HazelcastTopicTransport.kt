package com.orbitalhq.pipelines.jet.api.transport.hazelcast

import com.orbitalhq.pipelines.jet.api.transport.PipelineDirection
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpecId
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportType
import com.orbitalhq.schemas.QualifiedName

object HazelcastTopicTransport {
   const val TYPE: PipelineTransportType = "hazelcastTopic"
   val OUTPUT = HazelcastTopicSinkSpec.specId
}

data class HazelcastTopicSinkSpec(
   val topicName: String
) : PipelineTransportSpec {
   companion object {
      val specId =
         PipelineTransportSpecId(
            HazelcastTopicTransport.TYPE,
            PipelineDirection.OUTPUT,
            HazelcastTopicSinkSpec::class.java
         )

      const val reliableTopicPrefix = "managedStream."

      fun topicNameForStream(streamName: QualifiedName):String {
         return "$reliableTopicPrefix${streamName.parameterizedName}.results"
      }

      fun forStream(streamName: QualifiedName):HazelcastTopicSinkSpec {
         return HazelcastTopicSinkSpec(topicNameForStream(streamName))
      }
   }

   override val type: PipelineTransportType = HazelcastTopicTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.OUTPUT
   override val description: String = "Hazelcast topic sink"
}
