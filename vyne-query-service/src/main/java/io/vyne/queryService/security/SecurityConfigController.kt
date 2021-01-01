package io.vyne.queryService.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class SecurityConfigController(
   @Value("\${vyne.security.issuerUrl:}") private val issuerUrl: String,
   @Value("\${vyne.security.clientId:}") private val clientId: String,
   @Value("\${vyne.security.scope:}") private val scope: String) {
   @GetMapping("/api/security/config")
   fun securityIssuerUrl() = FrontendConfig(issuerUrl, clientId, scope)

   @GetMapping("/api/user")
   fun currentUserInfo(
      auth: Authentication?
   ): VyneUser {
      if (auth == null) {
         throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No user is currently logged in")
      } else {
         return auth.toVyneUser()
      }
   }
}

data class FrontendConfig(val issuerUrl: String, val clientId: String, val scope: String, val enabled: Boolean = issuerUrl.isNotBlank())


fun Authentication.toVyneUser(): VyneUser {
   return when (this) {
      is JwtAuthenticationToken -> this.toVyneUser()
      else -> TODO("Unhandled authentication type: ${this::class.simpleName}")
   }
}

fun JwtAuthenticationToken.toVyneUser(): VyneUser {
   val claims = this.token.claims

   fun <T> claim(fieldName: String): T? {
      return claims[fieldName] as T

   }

   fun <T> mandatoryClaim(fieldName: String): T {
      val claimValue = claim(fieldName) as Any?
         ?: error("JWT Token is malformed.  The $fieldName attribute is mandatory, but was not provided")
      return claimValue as T;
   }

   return VyneUser(
      userId = this.token.subject,
      username = mandatoryClaim("preferred_username"),
      email = mandatoryClaim("email"),
      profileUrl = claim("picture"),
      name = claim("name")
   )
}
