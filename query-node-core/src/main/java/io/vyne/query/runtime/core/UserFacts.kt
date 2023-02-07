package io.vyne.query.runtime.core

import io.vyne.FactSets
import io.vyne.UserType
import io.vyne.auth.authentication.VyneUser
import io.vyne.query.Fact


fun VyneUser?.facts(): Set<Fact> {
   return if (this == null) {
      emptySet()
   } else {
      setOf(
         Fact(UserType.USERNAME.fullyQualifiedName, this.username, FactSets.CALLER)
      )
   }
}
