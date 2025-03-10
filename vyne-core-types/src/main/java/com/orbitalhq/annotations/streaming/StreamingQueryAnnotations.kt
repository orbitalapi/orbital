package com.orbitalhq.annotations.streaming

import com.orbitalhq.VyneTypes

object StreamingQueryAnnotations {
   val namespace = "${VyneTypes.NAMESPACE}.streams"
   val StreamConsumerAnnotationName = "$namespace.StreamConsumer"
   val ParallelAnnotationName = "$namespace.Parallel"
   val schema = """
namespace $namespace {
   [[ Allows consumers to customize the id used when
   connecting to a message broker.
   ]]
   annotation StreamConsumer {
      [[ An id to use to identify this consumer to the message broker.
      For Kafka, this becomes the ConsumerGroupId
      ]]
      id : String
   }

   [[ Indicates that a streaming query should be distributed
   across the cluster, with multiple parallel consumers
   ]]
   annotation Parallel {
      count : Int
   }
}
   """.trimIndent()
}
