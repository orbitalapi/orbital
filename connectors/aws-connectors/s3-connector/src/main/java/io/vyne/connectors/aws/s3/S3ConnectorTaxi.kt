package io.vyne.connectors.aws.s3

import io.vyne.annotations.AnnotationWrapper
import io.vyne.schemas.Metadata
import io.vyne.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation

object S3ConnectorTaxi {
   const val S3EntryKeyTypeName = "S3EntryKey"
   val S3EntryKeyTypeFullyQualifiedName = "${Annotations.namespace}.$S3EntryKeyTypeName".fqn()
   val schema = """
namespace  ${Annotations.namespace} {
   annotation ${Annotations.S3Service.NAME.fqn().name} {
      connectionName : ConnectionName inherits String
   }

   annotation ${Annotations.S3Operation.NAME.fqn().name} {
      bucket : BucketName inherits String
   }

   type $S3EntryKeyTypeName inherits String
}
"""
   object Annotations {
      internal const val namespace = "io.vyne.aws.s3"
      data class S3Service(val connectionName: String) : AnnotationWrapper {
         companion object {
            const val NAME = "$namespace.S3Service"
            fun from(annotation: Annotation): S3Service {
               require(annotation.qualifiedName == NAME) { "Annotation name should be $NAME" }
               return S3Service(
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

      data class S3Operation(val bucket: String) : AnnotationWrapper {
         companion object {
            const val bucketMetadataName = "bucket"
            const val NAME = "$namespace.S3Operation"
            fun from(annotation: Annotation): S3Operation {
               return from(annotation.parameters)
            }

            fun from(annotation: Metadata): S3Operation {
               return from(annotation.params)
            }

            private fun from(parameters: Map<String, Any?>): S3Operation {
               return S3Operation(
                  bucket = parameters[bucketMetadataName] as String
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
            bucketMetadataName to bucket
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
