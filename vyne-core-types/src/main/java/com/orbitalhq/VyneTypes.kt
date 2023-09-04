package com.orbitalhq

import com.orbitalhq.schemas.fqn

object VyneTypes {
   const val NAMESPACE = "com.orbitalhq"
}

object UserType {
   val USERNAME = "${VyneTypes.NAMESPACE}.Username".fqn()
   val USERNAME_TYPEDEF = """namespace ${USERNAME.namespace} {
         |   type ${USERNAME.name} inherits String
         |}""".trimMargin()
}
