package com.orbitalhq.auth

import com.orbitalhq.AuthClaimType
import com.orbitalhq.FactSets
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.query.Fact
import com.orbitalhq.schemas.Schema

/**
 * Provides the claims in the user token represented as a series of facts for all types
 * that subtype from the AuthClaimType type.
 * This allows users who are authoring policies to provide the auth claims about the user as a
 * TypedInstance.
 *
 */
fun VyneUser?.getAuthClaimsAsFacts(schema: Schema): List<Fact> {
   if (this == null) {
      return emptyList()
   }
   val authClaimsBaseType = schema.type(AuthClaimType.AuthClaimsTypeName)
   val userAuthSubtypes = schema.types
      .filter { it.inheritsFrom(authClaimsBaseType) && it != authClaimsBaseType}
   return userAuthSubtypes.map { userAuthSubtype ->
      Fact(userAuthSubtype.paramaterizedName, this.claims, FactSets.AUTHENTICATION)
   }
}
