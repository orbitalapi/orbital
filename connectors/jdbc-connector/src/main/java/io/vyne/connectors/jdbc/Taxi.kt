package io.vyne.connectors.jdbc

import lang.taxi.types.QualifiedName

object Taxi {
   object Annotations {
      internal const val namespace = "io.vyne.jdbc"
      const val DatabaseOperation = "$namespace.DatabaseService"
      const val Table = "$namespace.Table"

      val databaseOperationName = QualifiedName.from(DatabaseOperation)
      val tableName = QualifiedName.from(Table)

      val imports: String = listOf(DatabaseOperation, Table).joinToString("\n") { "import $it"}
   }

   val schema = """
      namespace ${Annotations.namespace}
      annotation ${Annotations.databaseOperationName.typeName} {
         connectionName : String
      }

      annotation ${Annotations.tableName.typeName} {
         name : String
      }
   """


}
