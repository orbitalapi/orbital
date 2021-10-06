package io.vyne.queryService.catalog

import io.vyne.schemas.fqn

object DataOwnerAnnotations {
   internal const val namespace = "io.vyne.catalog"
   const val DataOwner = "$namespace.DataOwner"
   val dataOwnerName = DataOwner.fqn()

   val schema = """
namespace $namespace {
   annotation ${dataOwnerName.name} {
      id : io.vyne.Username
      name : String
   }
}
   """.trimIndent()
}
