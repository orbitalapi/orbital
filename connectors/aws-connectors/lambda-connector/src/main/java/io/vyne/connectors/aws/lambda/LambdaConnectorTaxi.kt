package com.orbitalhq.connectors.aws.lambda

import com.orbitalhq.annotations.AnnotationWrapper
import com.orbitalhq.connections.ConnectionUsageMetadataRegistry
import com.orbitalhq.connections.ConnectionUsageRegistration
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation

object LambdaConnectorTaxi {
   fun registerConnectorUsage() {
      ConnectionUsageMetadataRegistry.register(
         ConnectionUsageRegistration(Annotations.LambdaInvocationService.NAME.fqn(), "connection")
      )
   }
   val schema = """
namespace  ${Annotations.namespace} {
   annotation ${Annotations.LambdaInvocationService.NAME.fqn().name} {
      connectionName : ConnectionName inherits String
   }

   annotation ${Annotations.LambdaOperation.NAME.fqn().name} {
      name : OperationName inherits String
   }

}
"""

   object Annotations {
      internal const val namespace = "com.orbitalhq.aws.lambda"
      val imports: String = listOf(LambdaOperation.NAME, LambdaInvocationService.NAME).joinToString("\n") { "import $it" }

      data class LambdaInvocationService(val connectionName: String) : AnnotationWrapper {
         companion object {
            const val NAME = "$namespace.AwsLambdaService"

            fun from(annotation: Annotation): LambdaInvocationService {
               require(annotation.qualifiedName == NAME) { "Annotation name should be $NAME" }
               return LambdaInvocationService(
                  annotation.parameters["connection"] as String
               )
            }
         }

         override fun asAnnotation(schema: TaxiDocument): Annotation {
            return Annotation(
               type = schema.annotation(NAME),
               parameters = mapOf(
                  "connection" to connectionName
               )
            )
         }
      }

      data class LambdaOperation(val name: String) : AnnotationWrapper {
         companion object {
            const val operationMetadataName = "name"
            const val NAME = "$namespace.LambdaOperation"
            fun from(annotation: Annotation): LambdaOperation {
               return from(annotation.parameters)
            }

            fun from(annotation: Metadata): LambdaOperation {
               return from(annotation.params)
            }

            private fun from(parameters: Map<String, Any?>): LambdaOperation {
               return LambdaOperation(
                  name = parameters[operationMetadataName] as String
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
            operationMetadataName to name
         )

         override fun asAnnotation(schema: TaxiDocument): Annotation {
            return Annotation(
               type = schema.annotation(NAME),
               parameters = parameterMap
            )
         }
      }
   }
}
