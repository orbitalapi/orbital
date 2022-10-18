package io.vyne.queryService.security

import io.vyne.FactSets
import io.vyne.auth.authentication.VyneUser
import io.vyne.query.Fact
import io.vyne.queryService.schemas.VyneTypes
import io.vyne.schemas.fqn


object UserFacts {
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
         Fact(UserFacts.USERNAME.fullyQualifiedName, this.username, FactSets.CALLER)
      )
   }
}


