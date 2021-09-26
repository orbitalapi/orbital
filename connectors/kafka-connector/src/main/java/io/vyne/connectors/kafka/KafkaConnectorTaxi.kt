package io.vyne.connectors.kafka

import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation
import lang.taxi.types.QualifiedName

object KafkaConnectorTaxi {
   object Annotations {
      internal const val namespace = "io.vyne.kafka"
      const val KafkaOperation = "$namespace.KafkaService"

      val kafkaName = QualifiedName.from(KafkaOperation)

      val imports: String = listOf(KafkaOperation).joinToString("\n") { "import $it"}

      fun kafkaOperation(topic: String, schema: TaxiDocument): Annotation {
         return Annotation(
            type = schema.annotation(KafkaOperation),
            parameters = mapOf(
               "topic" to topic
            )
         )
      }

   }

   val schema = """
namespace  ${Annotations.namespace} {
   annotation ${Annotations.kafkaName.typeName} {
      topic : TopicName inherits String
   }
}
"""

}
