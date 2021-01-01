package io.vyne.queryService.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object JwtTokenAuthenticationFactConverter {
   fun extractUserName(): String? {
     return SecurityContextHolder.getContext()?.authentication?.let { authentication ->
        if (authentication is JwtAuthenticationToken) {
            authentication.tokenAttributes["preferred_username"]?.toString()
        } else {
           authentication.name
        }
      }
   }


}
