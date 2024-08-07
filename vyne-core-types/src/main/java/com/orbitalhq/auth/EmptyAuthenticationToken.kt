package com.orbitalhq.auth

import java.security.Principal


/**
 * Used for Mono / Flux methods that return empty.
 * It's discouraged to return null in these methods, so instead we wrap to an
 * EmptyAuthenticationToken, which we later unwrap back to null
 */
object EmptyAuthenticationToken : Principal {
   override fun getName(): String = "Anonymous"

   fun nullIfEmpty(principal: Principal): Principal? {
      return if (principal is EmptyAuthenticationToken) {
         null
      } else principal
   }
}

