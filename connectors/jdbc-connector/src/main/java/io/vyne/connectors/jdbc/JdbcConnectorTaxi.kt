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
               "connectionName" to connectionName
            )
         )
      }

      fun table(schemaName: String, tableName: String, schema: TaxiDocument): Annotation {
         return Annotation(
            type = schema.annotation(Table),
            parameters = mapOf(
               "name" to tableName,
               "schema" to schemaName
            )
         )
      }
   }

   val schema = """
namespace ${Annotations.namespace} {
   annotation ${Annotations.databaseOperationName.typeName} {
      connectionName : ConnectionName inherits String
   }

   annotation ${Annotations.tableName.typeName} {
      name : TableName inherits String
      schema: SchemaName inherits String
   }
}
   """


}
