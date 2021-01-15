package io.vyne.queryService.security

import io.vyne.FactSets
import io.vyne.query.Fact
import io.vyne.queryService.schemas.VyneTypes
import io.vyne.schemas.fqn
import io.vyne.security.VyneUser

object VyneUsers {
   val USERNAME = "${VyneTypes.NAMESPACE}.Username".fqn()
   val USERNAME_TYPEDEF = """namespace ${USERNAME.namespace} {
         |   type ${USERNAME.name} inherits String
         |}""".trimMargin()
}

fun VyneUser?.facts(): Set<Fact> {
   return if (this == null) {
      emptySet()
   } else {
      setOf(
         Fact(VyneUsers.USERNAME.fullyQualifiedName, username, FactSets.CALLER)
      )
   }

}
