package com.orbitalhq.cockpit.core.catalog

import com.orbitalhq.schemas.fqn

object DataOwnerAnnotations {
   internal const val namespace = "com.orbitalhq.catalog"
   const val DataOwner = "$namespace.DataOwner"
   val dataOwnerName = DataOwner.fqn()

   val schema = """
namespace $namespace {
   annotation ${dataOwnerName.name} {
      id : com.orbitalhq.Username
      name : String
   }
}
   """.trimIndent()
}
