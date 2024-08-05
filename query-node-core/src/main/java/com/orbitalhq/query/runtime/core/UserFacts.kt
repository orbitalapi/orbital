package com.orbitalhq.query.runtime.core

import com.orbitalhq.FactSets
import com.orbitalhq.UserType
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.Fact


fun VyneUser?.facts(jwtClaimType: String? = null): Set<Fact> {
   return if (this == null) {
      emptySet()
   } else {
      val claimFact =  if (jwtClaimType != null) {
         setOf(
            Fact(jwtClaimType, this.claims, FactSets.CALLER)
         )
      } else emptySet()
      setOf(
         Fact(UserType.USERNAME.fullyQualifiedName, this.username, FactSets.CALLER)
      ) + claimFact
   }
}
