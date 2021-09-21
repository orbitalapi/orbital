package io.vyne.connectors.kafka

import lang.taxi.types.QualifiedName

object Taxi {
   object Annotations {
      internal const val namespace = "io.vyne.kafka"
      const val KafkaOperation = "$namespace.KafkaService"
      const val Table = "$namespace.Table"

      val databaseOperationName = QualifiedName.from(KafkaOperation)
      val tableName = QualifiedName.from(Table)

      val imports: String = listOf(KafkaOperation, Table).joinToString("\n") { "import $it"}
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
