package io.vyne.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class AuthenticationToVyneUserConverter : Converter<Authentication, VyneUser> {
   override fun convert(source: Authentication): VyneUser {
      return source.toVyneUser()
   }
}

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

