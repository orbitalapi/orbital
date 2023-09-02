package com.orbitalhq.query.runtime.core

import com.orbitalhq.FactSets
import com.orbitalhq.UserType
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.query.Fact


fun VyneUser?.facts(): Set<Fact> {
   return if (this == null) {
      emptySet()
   } else {
      setOf(
         Fact(UserType.USERNAME.fullyQualifiedName, this.username, FactSets.CALLER)
      )
   }
}
