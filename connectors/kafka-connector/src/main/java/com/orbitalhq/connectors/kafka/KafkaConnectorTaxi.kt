package com.orbitalhq.connectors.kafka

import com.orbitalhq.annotations.AnnotationWrapper
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation

object KafkaConnectorTaxi {
   object Annotations {
      internal const val namespace = "com.orbitalhq.kafka"

      val imports: String = listOf(KafkaService.NAME, KafkaOperation.NAME).joinToString("\n") { "import $it" }

      data class KafkaService(val connectionName: String): AnnotationWrapper {
         companion object {
            const val NAME = "$namespace.KafkaService"
         }

         val parameterMap = mapOf("connectionName" to connectionName)
         override fun asAnnotation(schema: TaxiDocument): Annotation {
            return Annotation(
               schema.annotation(NAME),
               parameterMap
            )
         }

         fun asMetadata():Metadata {
            return com.orbitalhq.schemas.Metadata(
               NAME.fqn(),
               parameterMap
            )
         }
      }
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

         fun asMetadata(): Metadata {
            return Metadata(
               name = NAME.fqn(),
               params = parameterMap
            )
         }

         private val parameterMap: Map<String, Any> = mapOf(
            "topic" to topic,
            "offset" to offset.name.toLowerCase()
         )

         override fun asAnnotation(schema: TaxiDocument): Annotation {
            return Annotation(
               type = schema.annotation(NAME),
               parameters = parameterMap
            )
         }

      }
   }

   val schema = """
namespace  ${Annotations.namespace} {
   annotation ${Annotations.KafkaService.NAME.fqn().name} {
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
