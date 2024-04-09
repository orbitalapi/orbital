package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.annotations.AnnotationWrapper
import com.orbitalhq.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation
import lang.taxi.types.QualifiedName

object MongoConnector {
   object Annotations {
      internal const val namespace = "com.orbitalhq.mongo"
      val UpsertOperationAnnotationName = "${namespace}.UpsertOperation".fqn()
      val ObjectIdAnnotationName = "${namespace}.ObjectId".fqn()
      data class MongoOperation(val connectionName: String) : AnnotationWrapper {
         companion object {
            const val NAME = "$namespace.MongoService"


            fun from(annotation: Annotation): MongoOperation {
               require(annotation.qualifiedName == NAME) { "Annotation name should be $NAME" }
               return MongoOperation(
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

      data class Collection(val collectionName: String, val connectionName: String) : AnnotationWrapper {
         companion object {
            const val NAME = "$namespace.Collection"
            fun from(annotation: Annotation): Collection {
               require(annotation.qualifiedName == NAME) { "Annotation name should be $NAME" }
               return Collection(
                  collectionName = annotation.parameters["collection"] as String,
                  connectionName = annotation.parameters["connection"] as String
               )
            }



         }

         override fun asAnnotation(schema: TaxiDocument): Annotation {
            return Annotation(
               type = schema.annotation(NAME),
               parameters = mapOf(
                  "collection" to collectionName,
                  "connection" to connectionName
               )
            )
         }
      }
      val mongoOperationName = QualifiedName.from(MongoOperation.NAME)
      val collectionName = QualifiedName.from(Collection.NAME)

      val imports: String = listOf(MongoOperation.NAME, Collection.NAME, ObjectIdAnnotationName).joinToString("\n") { "import $it" }
   }

   val schema = """
namespace ${Annotations.namespace} {
   type ConnectionName inherits String
   annotation ${Annotations.mongoOperationName.typeName} {
      connection : ConnectionName
   }
   
   annotation UpsertOperation {}
   annotation ObjectId {}
  
   annotation ${Annotations.collectionName.typeName} {
      connection : ConnectionName
      collection : CollectionName inherits String
   }
}
   """
}

