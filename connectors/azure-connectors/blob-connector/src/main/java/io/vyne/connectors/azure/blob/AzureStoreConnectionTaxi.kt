package io.vyne.connectors.azure.blob

import io.vyne.annotations.AnnotationWrapper
import io.vyne.schemas.Metadata
import io.vyne.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation

object AzureStoreConnectionTaxi {
   const val AzureStoreBlobName = "AzureStoreBlob"
   val AzureStoreBlobTypeFullyQualifiedName = "${Annotations.namespace}.$AzureStoreBlobName".fqn()
   val schema = """
namespace  ${Annotations.namespace} {
   annotation ${Annotations.AzureStoreService.NAME.fqn().name} {
      connectionName : ConnectionName inherits String
   }

   annotation ${Annotations.StoreOperation.NAME.fqn().name} {
      container : AzureStoreContainer inherits String
   }

   type $AzureStoreBlobName inherits String
}
"""
   object Annotations {
      internal const val namespace = "io.vyne.azure.store"
      val imports: String = listOf(StoreOperation.NAME, AzureStoreService.NAME, AzureStoreBlobTypeFullyQualifiedName).joinToString("\n") { "import $it" }
      data class AzureStoreService(val connectionName: String) : AnnotationWrapper {
         companion object {
            const val NAME = "$namespace.BlobService"
            fun from(annotation: Annotation): AzureStoreService {
               require(annotation.qualifiedName == NAME) { "Annotation name should be $NAME" }
               return AzureStoreService(
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

      data class StoreOperation(val container: String) : AnnotationWrapper {
         companion object {
            const val containerMetadataName = "container"
            const val NAME = "$namespace.AzureStoreOperation"
            fun from(annotation: Annotation): StoreOperation {
               return from(annotation.parameters)
            }

            fun from(annotation: Metadata): StoreOperation {
               return from(annotation.params)
            }

            private fun from(parameters: Map<String, Any?>): StoreOperation {
               return StoreOperation(
                  container = parameters[containerMetadataName] as String
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
            containerMetadataName to container
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
