package io.vyne.connectors.aws.sqs

import io.vyne.annotations.AnnotationWrapper
import io.vyne.schemas.Metadata
import io.vyne.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation

object SqsConnectorTaxi {
   object Annotations {
      internal const val namespace = "io.vyne.aws.sqs"

      val imports: String = listOf(SqsService.NAME, SqsOperation.NAME).joinToString("\n") { "import $it" }

      data class SqsService(val connectionName: String): AnnotationWrapper {
         companion object {
            const val ConnectionNameParam = "connectionName"
            const val NAME = "$namespace.SqsService"
         }

         private val parameterMap = mapOf(ConnectionNameParam to connectionName)
         override fun asAnnotation(schema: TaxiDocument): Annotation {
            return Annotation(
               schema.annotation(NAME),
               parameterMap
            )
         }
      }
      data class SqsOperation(val queue: String) : AnnotationWrapper {
         companion object {
            const val QueueParamName = "queue"
            const val NAME = "$namespace.SqsOperation"
            fun from(annotation: Annotation): SqsOperation {
               return from(annotation.parameters)
            }

            fun from(annotation: Metadata): SqsOperation {
               return from(annotation.params)
            }

            private fun from(parameters: Map<String, Any?>): SqsOperation {
               return SqsOperation(
                  queue = parameters[QueueParamName] as String
               )
            }
         }

         private val parameterMap: Map<String, Any> = mapOf(
            QueueParamName to queue
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
   annotation ${Annotations.SqsService.NAME.fqn().name} {
      connectionName : ConnectionName inherits String
   }
   annotation ${Annotations.SqsOperation.NAME.fqn().name} {
      queue : QueueName inherits String
   }
}
"""
}
