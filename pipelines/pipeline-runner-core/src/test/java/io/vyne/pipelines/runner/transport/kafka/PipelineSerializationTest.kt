package io.vyne.pipelines.runner.transport.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import io.vyne.schemas.fqn
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.util.*

class PipelineSerializationTest {

   private fun producerProps(): Map<String, Any> {
      val props: MutableMap<String, Any> = HashMap<String, Any>()
      props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "127.0.0.1:9092"
      props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
      return props
   }

   private fun consumerProps(groupId: String): Map<String, Any> {
      val props: MutableMap<String, Any> = HashMap<String, Any>()
      props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "127.0.0.1:9092"
      props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
      return props
   }

   private lateinit var objectMapper: ObjectMapper

   @Before
   fun setup() {
      objectMapper = jacksonObjectMapper().registerModule(PipelineJacksonModule())
   }

   @Test
   fun canDeserializePipeline() {
      val pipelineJson = """
            {
              "name" : "test-pipeline",
              "input" : {
                "type" : "PersonLoggedOnEvent",
                "transport" : {
                  "topic" : "pipeline-input",
                  "targetType" : "PersonLoggedOnEvent",
                  "props" : {
                    "group.id" : "vyne-pipeline-group",
                    "bootstrap.servers" : "127.0.0.1:9092"
                  },
                  "direction" : "INPUT",
                  "type" : "kafka"
                }
              },
              "output" : {
                "type" : "UserEvent",
                "transport" : {
                  "targetType" : "UserEvent",
                  "direction" : "OUTPUT",
                  "type" : "cask",
                  "props" : {
                  }
               }
              }
            }
      """.trimIndent()

      val pipeline = objectMapper.readValue(pipelineJson, Pipeline::class.java)

      pipeline.id.should.startWith("test-pipeline")
      pipeline.name.should.equal("test-pipeline")
      pipeline.input.transport.type.should.equal("kafka")
      pipeline.output.transport.type.should.equal("cask")

   }

   @Test
   fun canSerializePipeline() {

      val pipeline = Pipeline(
         "test-pipeline",
         PipelineChannel(
            VersionedTypeReference("PersonLoggedOnEvent".fqn()),
            KafkaTransportInputSpec(topic = "pipeline-input", targetType = VersionedTypeReference("PersonLoggedOnEvent".fqn()), props = consumerProps("vyne-pipeline-group")
            )
         ),
         PipelineChannel(
            VersionedTypeReference("UserEvent".fqn()),
            KafkaTransportOutputSpec(topic = "pipeline-output", targetType = VersionedTypeReference("UserEvent".fqn()), props = producerProps()
            )
         )
      )

      val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(pipeline)

      val pipelineJson = """{
  "name" : "test-pipeline",
  "input" : {
    "type" : "PersonLoggedOnEvent",
    "transport" : {
      "topic" : "pipeline-input",
      "targetType" : "PersonLoggedOnEvent",
      "props" : {
        "group.id" : "vyne-pipeline-group",
        "bootstrap.servers" : "127.0.0.1:9092"
      },
      "type" : "kafka",
      "direction" : "INPUT"
    }
  },
  "output" : {
    "type" : "UserEvent",
    "transport" : {
      "topic" : "pipeline-output",
      "props" : {
        "bootstrap.servers" : "127.0.0.1:9092",
        "key.serializer" : "org.apache.kafka.common.serialization.StringSerializer"
      },
      "targetType" : "UserEvent",
      "type" : "kafka",
      "direction" : "OUTPUT"
    }
  },
  "id" : "${pipeline.id}"
}"""

      JSONAssert.assertEquals(pipelineJson, json, JSONCompareMode.LENIENT);
   }
}
