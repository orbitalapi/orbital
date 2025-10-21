package com.orbitalhq.connectors.jdbc

import com.orbitalhq.VyneTypes
import com.orbitalhq.annotations.AnnotationWrapper
import com.orbitalhq.connections.ConnectionUsageMetadataRegistry
import com.orbitalhq.connections.ConnectionUsageRegistration
import com.orbitalhq.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation
import lang.taxi.types.QualifiedName

object JdbcConnectorTaxi {
   fun registerConnectionUsage() {
         ConnectionUsageMetadataRegistry.register(
            ConnectionUsageRegistration(Annotations.DatabaseOperation.NAME.fqn(), "connection")
         )
      ConnectionUsageMetadataRegistry.register(
         ConnectionUsageRegistration(Annotations.DatabaseOperation.NAME.fqn(), Annotations.DatabaseOperation::connectionName.name)
      )
   }
   object Annotations {
      internal val namespace = "${VyneTypes.NAMESPACE}.jdbc"
      val Column = "$namespace.Column"

      val UpsertOperationAnnotationName = "$namespace.UpsertOperation".fqn()
      val InsertOperationAnnotationName = "$namespace.InsertOperation".fqn()
      val UpdateOperationAnnotationName = "$namespace.UpdateOperation".fqn()

      val batchSizeParamName: String = "batchSize"
      val batchDurationParamName: String = "batchDuration"

      const val GeneratedIdAnnotationName = "GeneratedId"

      data class DatabaseOperation(val connectionName: String) : AnnotationWrapper {
         companion object {
            val NAME = "$namespace.DatabaseService"
            fun from(annotation: Annotation): DatabaseOperation {
               require(annotation.qualifiedName == NAME) { "Annotation name should be $NAME" }
               return DatabaseOperation(
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

      data class Table(val schemaName: String, val tableName: String, val connectionName: String) : AnnotationWrapper {
         companion object {
            val NAME = "${VyneTypes.NAMESPACE}.jdbc.Table"
            fun from(annotation: Annotation): Table {
               require(annotation.qualifiedName == NAME) { "Annotation name should be $NAME" }
               return Table(
                  schemaName = annotation.parameters["schema"] as String,
                  tableName = annotation.parameters["table"] as String,
                  connectionName = annotation.parameters["connection"] as String
               )
            }



         }

         override fun asAnnotation(schema: TaxiDocument): Annotation {
            return Annotation(
               type = schema.annotation(NAME),
               parameters = mapOf(
                  "table" to tableName,
                  "schema" to schemaName,
                  "connection" to connectionName
               )
            )
         }
      }

      val databaseOperationName = QualifiedName.from(DatabaseOperation.NAME)
      val tableName = QualifiedName.from(Table.NAME)
      val columnName = QualifiedName.from(Column)

      val imports: String = listOf(DatabaseOperation.NAME, Table.NAME,
         UpsertOperationAnnotationName.parameterizedName,
         InsertOperationAnnotationName.parameterizedName,
         UpdateOperationAnnotationName.parameterizedName
         ).joinToString("\n") { "import $it" }

      fun databaseOperation(connectionName: String): DatabaseOperation {
         return DatabaseOperation(connectionName)
      }

      fun table(schemaName: String, tableName: String, connectionName: String): Table {
         return Table(schemaName, tableName, connectionName)
      }
   }

   val schema = """
namespace ${Annotations.namespace} {
   type ConnectionName inherits String
   annotation UpsertOperation {
        ${Annotations.batchSizeParamName}: Int?
        ${Annotations.batchDurationParamName}: Int?
   }
   annotation InsertOperation {
        ${Annotations.batchSizeParamName}: Int?
        ${Annotations.batchDurationParamName}: Int?
   }
   annotation UpdateOperation {
        ${Annotations.batchSizeParamName}: Int?
        ${Annotations.batchDurationParamName}: Int?
   }
   annotation ${Annotations.databaseOperationName.typeName} {
      connection : ConnectionName
   }

   annotation ${Annotations.tableName.typeName} {
      connection : ConnectionName
      table : TableName inherits String
      schema: SchemaName inherits String

   }
}
   """


}
