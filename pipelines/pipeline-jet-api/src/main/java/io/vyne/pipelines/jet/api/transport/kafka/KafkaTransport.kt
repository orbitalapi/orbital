package io.vyne.pipelines.jet.api.transport.kafka

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.documentation.Maturity
import io.vyne.pipelines.jet.api.documentation.PipelineDocs
import io.vyne.pipelines.jet.api.documentation.PipelineDocumentationSample
import io.vyne.pipelines.jet.api.documentation.PipelineParam
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType

object KafkaTransport {
   const val TYPE: PipelineTransportType = "kafka"
   val INPUT = KafkaTransportInputSpec.specId
   val OUTPUT = KafkaTransportOutputSpec.specId
}

@PipelineDocs(
   name = "Kafka topic",
   docs = """Defines an input that reads from a Kafka topic.

The kafka broker is configured using Vyne's connection manager, along with a topic
defined for this pipeline input.

#### Controlling deserialization (protobuf / avro etc)
Deserialization is controlled using annotations declared on the configured type (`targetTypeName`).

If not specified, Vyne attempts to read the content as JSON, using a `StringDecoder`
""",
   maturity = Maturity.BETA,
   sample = KafkaTransportInputSpec.Sample::class
)
open class KafkaTransportInputSpec(
   @PipelineParam("The name of the connection, as registered in Vyne's connection manager")
   val connectionName: String,
   @PipelineParam("The name of the topic to consume from")
   val topic: String,
   @PipelineParam("The fully qualified name of the type that content should be read as.")
   val targetTypeName: String,
) : PipelineTransportSpec {

   object Sample : PipelineDocumentationSample<KafkaTransportInputSpec> {
      override val sample = KafkaTransportInputSpec(
         connectionName = "my-kafka-connection",
         topic = "customerNotifications",
         targetTypeName = "com.demo.CustomerEvent"
      )
   }

   companion object {
      val specId =
         PipelineTransportSpecId(KafkaTransport.TYPE, PipelineDirection.INPUT, KafkaTransportInputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(targetTypeName)
      }

   override val description: String = "Kafka input from topic $topic on connection $connectionName"
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = KafkaTransport.TYPE
}


@PipelineDocs(
   name = "Kafka topic",
   docs = """Defines an output that writes to a Kafka topic.

The kafka broker is configured using Vyne's connection manager, along with a topic
defined for this pipeline output.

#### Controlling serialization (protobuf / avro etc)
Serialization is controlled using annotations declared on the configured type (`targetTypeName`).

If not specified, Vyne attempts to write the content as JSON, using a `StringDecoder`
""",
   maturity = Maturity.BETA,
   sample = KafkaTransportOutputSpec.Sample::class
)
data class KafkaTransportOutputSpec(
   @PipelineParam("The name of the connection, as registered in Vyne's connection manager")
   val connectionName: String,
   @PipelineParam("The name of the topic to write to")
   val topic: String,
   @PipelineParam("The fully qualified name of the type that content should be written as.")
   val targetTypeName: String
) : PipelineTransportSpec {
   companion object {
      val specId =
         PipelineTransportSpecId(KafkaTransport.TYPE, PipelineDirection.OUTPUT, KafkaTransportOutputSpec::class.java)
   }

   object Sample : PipelineDocumentationSample<KafkaTransportOutputSpec> {
      override val sample = KafkaTransportOutputSpec(
         connectionName = "my-kafka-connection",
         topic = "CustomerEvents",
         targetTypeName = "com.demo.customers.CustomerEvent"
      )
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(this.targetTypeName)
      }
   override val description: String = "Kafka output to topic $topic on connection $connectionName"

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = KafkaTransport.TYPE
}
