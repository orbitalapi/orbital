package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.VyneTypes
import com.orbitalhq.annotations.AnnotationWrapper
import com.orbitalhq.connections.ConnectionUsageMetadataRegistry
import com.orbitalhq.connections.ConnectionUsageRegistration
import com.orbitalhq.connectors.nosql.mongodb.MongoConnector.Annotations.BatchDurationAttribute
import com.orbitalhq.connectors.nosql.mongodb.MongoConnector.Annotations.BatchSizeAttribute
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation
import lang.taxi.types.QualifiedName

object MongoConnector {
   fun registerConnectionUsage() {
      ConnectionUsageMetadataRegistry.register(
         ConnectionUsageRegistration(Annotations.MongoOperation.NAME.fqn(), "connection")
      )
   }

   object Annotations {
      internal val namespace = "${VyneTypes.NAMESPACE}.mongo"
      val UpsertOperationAnnotationName = "${namespace}.UpsertOperation".fqn()
      const val BatchSizeAttribute = "BatchSize"
      const val BatchDurationAttribute = "BatchDuration"
      const val BatchSizeAttributeName = "batchSize"
      const val batchDurationAttributeName = "batchDuration"
      val ObjectIdAnnotationName = "${namespace}.ObjectId".fqn()
      val UniqueIndexAnnotationName = "${namespace}.UniqueIndex".fqn()
      val SetOnInsertAnnotationName = "${namespace}.SetOnInsert".fqn()

      data class MongoOperation(val connectionName: String) : AnnotationWrapper {
         companion object {
            val NAME = "$namespace.MongoService"

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
            val NAME = "$namespace.Collection"
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

      data class AggregationPipeline(
         val collection: String,
         val stages: List<String>
      ) {
         companion object {
            fun from(params: Map<String, Any?>): AggregationPipeline {
               val stages = params["stages"] as List<String>
               val collection = params["collection"] as String
               return AggregationPipeline(collection, stages)
            }
         }
      }


      data class AggregateTransaction(val pipelines: List<AggregationPipeline>, val transactional: Boolean) {
         companion object {
            val NAME = "$namespace.AggregateTransaction"

            fun from(annotation: Metadata): AggregateTransaction {
               require(annotation.name.parameterizedName == NAME) { "Annotation name should be $NAME" }
               val pipelinesCollectionParams = annotation.params["pipelines"] as List<Map<String, Any?>>
               val pipelines = pipelinesCollectionParams.map { pipelineParams ->
                  AggregationPipeline.from(pipelineParams)
               }
               val transactional = annotation.params["transactional"] as Boolean

               return AggregateTransaction(pipelines, transactional)
            }
         }
      }
      data class CollectionAggregation(val pipeline: AggregationPipeline) {
         companion object {
            val NAME = "$namespace.CollectionAggregation"

            fun from(annotation: Metadata): CollectionAggregation {
               require(annotation.name.parameterizedName == NAME) { "Annotation name should be $NAME" }
               val pipelineParams = annotation.params["pipeline"] as Map<String, Any?>
               val pipeline = AggregationPipeline.from(pipelineParams)
               return CollectionAggregation(pipeline)
            }
         }
      }

      data class DeleteOperation(val batchSize: Int?, val batchDurationInMillis: Long?) {
         companion object {
            val NAME = "$namespace.DeleteOperation"

            fun from(annotation: Metadata): DeleteOperation {
               require(annotation.name.parameterizedName == NAME) { "Annotation name should be $NAME" }
               val batchSize = annotation.params["batchSize"] as Int?
               val batchDuration = annotation.params["batchDuration"] as Int?
               return DeleteOperation(batchSize, batchDuration?.toLong())
            }
         }
      }

      /**
       * The @MongoDelete annotation, which provides native mongo delete
       * filtering etc.
       *
       */
      data class DeleteByQuery(val filter: String, val collection: String) : AnnotationWrapper {
         companion object {
            val NAME = "$namespace.DeleteByQuery"

            fun from(annotation: Metadata): DeleteByQuery {
               require(annotation.name.parameterizedName == NAME) { "Annotation name should be $NAME" }
               val filter = annotation.params["filter"] as String
               val collection = annotation.params["collection"] as String
               return DeleteByQuery(filter, collection)
            }
         }

         override fun asAnnotation(schema: TaxiDocument): Annotation {
            return Annotation(
               type = schema.annotation(NAME),
               parameters = mapOf(
                  "filter" to filter,
                  "collection" to collection
               )
            )
         }
      }

      val mongoOperationName = QualifiedName.from(MongoOperation.NAME)
      val collectionName = QualifiedName.from(Collection.NAME)
      val CollectionAggregationName = QualifiedName.from(CollectionAggregation.NAME)
      val AggregateTransactionName = QualifiedName.from(AggregateTransaction.NAME)
      val DeleteOperationName = QualifiedName.from(DeleteOperation.NAME)
      val DeleteByQueryName = QualifiedName.from(DeleteByQuery.NAME)

      val imports: String = listOf(
         MongoOperation.NAME,
         Collection.NAME,
         ObjectIdAnnotationName,
         UniqueIndexAnnotationName,
         SetOnInsertAnnotationName,
         CollectionAggregation.NAME,
         DeleteOperation.NAME,
         AggregateTransactionName,
         DeleteByQuery.NAME
      ).joinToString("\n") { "import $it" }

      data class BatchAttribute(val batchSize: Int, val batchDurationInMillis: Long)
   }

   val schema = """
namespace ${Annotations.namespace} {
   type ConnectionName inherits String
   type $BatchSizeAttribute inherits Int
   type $BatchDurationAttribute inherits Int
   annotation ${Annotations.mongoOperationName.typeName} {
      connection : ConnectionName
   }

   annotation UpsertOperation {
        batchSize: $BatchSizeAttribute?
        batchDuration: $BatchDurationAttribute?
   }

   annotation ObjectId {}
   annotation UniqueIndex {}
   annotation SetOnInsert {}

   [[
      Marks a write operation as a delete operation.
      Supports batching with optional batchSize and batchDuration parameters.
   ]]
   annotation ${Annotations.DeleteOperationName.typeName} {
      batchSize: $BatchSizeAttribute?
      batchDuration: $BatchDurationAttribute?
   }

   annotation ${Annotations.collectionName.typeName} {
      connection : ConnectionName
      collection : CollectionName inherits String
   }

   [[
      Allows for defining a native Mongo query as an aggregate pipeline
   ]]
   annotation ${Annotations.CollectionAggregationName.typeName} {
     pipeline: AggregationPipeline
   }

   [[ Defines a collection of aggregation pipelines, which execute
   within a single transaction
   ]]
   annotation ${Annotations.AggregateTransactionName.typeName} {
      pipelines: AggregationPipeline[]

      [[ Indicates if the entire pipeline should be wrapped in a transaction.
      Mongo has [specific limitations](https://www.mongodb.com/docs/manual/core/transactions-operations/) around what is possible to do within a
      transaction, and Orbital does not verify that your pipeline does not break these.

      If you define a pipeline that is invalid, an error will be thrown at runtime
      ]]
      transactional: Boolean = true
   }

model AggregationPipeline {
   collection: String
   stages: String[]   // each string = a Mongo pipeline stage, e.g. '{ ${'$'}match: {...} }'
}

   [[
      Allows for defining a native MongoDB delete operation with custom filter criteria.
      Gives the full power of Mongo's query syntax, but steps outside of the Taxi type
      system. Use this for scenarios, where you want more control over the delete behaviour.

      For simple deletes, prefer @DeleteOperation
   ]]
   annotation ${Annotations.DeleteByQueryName.typeName} {
      collection: String
      filter: String
   }
}
   """
}
