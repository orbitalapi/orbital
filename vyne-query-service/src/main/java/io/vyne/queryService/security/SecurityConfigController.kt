package io.vyne.queryService.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SecurityConfigController(
   @Value("\${vyne.security.issuerUrl:}") private val issuerUrl: String,
   @Value("\${vyne.security.clientId:}") private val clientId: String,
   @Value("\${vyne.security.scope:}") private val scope: String) {

   @GetMapping("/api/security/config")
   fun securityIssuerUrl() = FrontendConfig(issuerUrl, clientId, scope)

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
      return claimValue as T
   }

   return VyneUser(
      userId = this.token.subject,
      username = mandatoryClaim(JwtStandardClaims.PreferredUserName),
      email = mandatoryClaim(JwtStandardClaims.Email),
      profileUrl = claim(JwtStandardClaims.PictureUrl),
      name = claim(JwtStandardClaims.Name),
      isAuthenticated = true
   )
}


/**
 * see https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
 *
 */
object JwtStandardClaims {
   // Subject - Identifier for the End-User at the Issuer.
   const val Sub = "sub"
   // End-User's full name in displayable form including all name parts, possibly including titles and suffixes, ordered according to the End-User's locale and preferences.
   const val Name = "name"
   // End-User's preferred e-mail address. Its value MUST conform to the RFC 5322 [RFC5322] addr-spec syntax. The RP MUST NOT rely upon this value being unique
   const val Email = "email"
   // Shorthand name by which the End-User wishes to be referred to at the RP, such as janedoe or j.doe.
   // This value MAY be any valid JSON string including special characters such as @, /, or whitespace.
   // The RP MUST NOT rely upon this value being unique
   const val PreferredUserName = "preferred_username"
   /**
    * URL of the End-User's profile picture.
    * This URL MUST refer to an image file (for example, a PNG, JPEG, or GIF image file),
    * rather than to a Web page containing an image.
    * Note that this URL SHOULD specifically reference a profile photo of the End-User suitable for displaying when describing the End-User,
    * rather than an arbitrary photo taken by the End-User.
    */
   const val PictureUrl = "picture"
}
