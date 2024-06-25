package com.orbitalhq.cockpit.core.catalog

import com.orbitalhq.VyneTypes
import com.orbitalhq.schemas.fqn

object DataOwnerAnnotations {
   internal  val namespace = "${VyneTypes.NAMESPACE}.catalog"
   val DataOwner = "$namespace.DataOwner"
   val dataOwnerName = DataOwner.fqn()

   val schema = """
namespace $namespace {
   annotation ${dataOwnerName.name} {
      id : ${VyneTypes.NAMESPACE}.Username
      name : String
   }
}
   """.trimIndent()
}
