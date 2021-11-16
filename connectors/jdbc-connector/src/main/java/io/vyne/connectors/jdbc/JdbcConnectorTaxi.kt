package io.vyne.connectors.jdbc

import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation
import lang.taxi.types.QualifiedName

object JdbcConnectorTaxi {
   object Annotations {
      internal const val namespace = "io.vyne.jdbc"
      const val DatabaseOperation = "$namespace.DatabaseService"
      const val Table = "$namespace.Table"
      const val Column = "$namespace.Column"

      val databaseOperationName = QualifiedName.from(DatabaseOperation)
      val tableName = QualifiedName.from(Table)
      val columnName = QualifiedName.from(Column)

      val imports: String = listOf(DatabaseOperation, Table).joinToString("\n") { "import $it" }

      fun databaseOperation(connectionName: String, schema: TaxiDocument): Annotation {
         return Annotation(
            type = schema.annotation(DatabaseOperation),
            parameters = mapOf(
               "connection" to connectionName
            )
         )
      }

      fun table(schemaName: String, tableName: String,  connectionName: String, schema: TaxiDocument,): Annotation {
         return Annotation(
            type = schema.annotation(Table),
            parameters = mapOf(
               "table" to tableName,
               "schema" to schemaName,
               "connection" to connectionName
            )
         )
      }
   }

   val schema = """
namespace ${Annotations.namespace} {
   annotation ${Annotations.databaseOperationName.typeName} {
      connection : ConnectionName inherits String
   }

   annotation ${Annotations.tableName.typeName} {
      connection : ConnectionName
      table : TableName inherits String
      schema: SchemaName inherits String

   }
}
   """


}
