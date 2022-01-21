package io.vyne.connectors.kafka

import io.vyne.annotations.AnnotationWrapper
import io.vyne.schemas.Metadata
import io.vyne.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation

object KafkaConnectorTaxi {
   object Annotations {
      internal const val namespace = "io.vyne.kafka"
      const val KafkaService = "$namespace.KafkaService"
      val imports: String = listOf(KafkaService, KafkaOperation.NAME).joinToString("\n") { "import $it" }

      data class KafkaOperation(val topic: String, val offset: Offset) : AnnotationWrapper {
         enum class Offset {
            EARLIEST,
            LATEST,
            NONE
         }

         companion object {
            const val NAME = "$namespace.KafkaOperation"
            fun from(annotation: Annotation): KafkaOperation {
               return from(annotation.parameters)
            }

            fun from(annotation: Metadata): KafkaOperation {
               return from(annotation.params)
            }

            private fun from(parameters: Map<String, Any?>): KafkaOperation {
               return KafkaOperation(
                  topic = parameters["topic"] as String,
                  offset = (parameters["offset"] as String).let { offset -> Offset.valueOf(offset.toUpperCase()) }
               )
            }
         }

         override fun asAnnotation(schema: TaxiDocument): Annotation {
            return Annotation(
               type = schema.annotation(NAME),
               parameters = mapOf(
                  "topic" to topic,
                  "offset" to offset.name.toLowerCase()
               )
            )
         }

      }
   }

   val schema = """
namespace  ${Annotations.namespace} {
   annotation ${Annotations.KafkaService.fqn().name} {
      connectionName : ConnectionName inherits String
   }
   enum TopicOffset {
      earliest,
      latest,
      none
   }
   annotation ${Annotations.KafkaOperation.NAME.fqn().name} {
      topic : TopicName inherits String
      offset : TopicOffset
   }
}
"""

}
