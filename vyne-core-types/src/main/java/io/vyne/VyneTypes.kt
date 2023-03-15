package io.vyne

import io.vyne.schemas.fqn

object VyneTypes {
   const val NAMESPACE = "io.vyne"
}

object UserType {
   val USERNAME = "${VyneTypes.NAMESPACE}.Username".fqn()
   val USERNAME_TYPEDEF = """namespace ${USERNAME.namespace} {
         |   type ${USERNAME.name} inherits String
         |}""".trimMargin()
}
