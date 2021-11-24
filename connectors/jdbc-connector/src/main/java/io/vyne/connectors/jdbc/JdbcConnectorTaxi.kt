package io.vyne.connectors.jdbc

import io.vyne.annotations.AnnotationWrapper
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation
import lang.taxi.types.QualifiedName

object JdbcConnectorTaxi {
   object Annotations {
      internal const val namespace = "io.vyne.jdbc"
      const val Column = "$namespace.Column"

      data class DatabaseOperation(val connectionName: String) : AnnotationWrapper {
         companion object {
            const val NAME = "$namespace.DatabaseService"
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
            const val NAME = "$namespace.Table"
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

      val imports: String = listOf(DatabaseOperation, Table).joinToString("\n") { "import $it" }

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
