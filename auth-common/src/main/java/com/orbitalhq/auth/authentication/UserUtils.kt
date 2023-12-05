package com.orbitalhq.auth.authentication

import com.orbitalhq.security.VyneGrantedAuthority
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

fun getPreferredUserDisplayName(claims: Map<String, Any>): String {
   fun hasClaims(vararg names: String): Boolean {
      return names.all { claims.containsKey(it) }
   }

   return when {
      hasClaims(JwtStandardClaims.PreferredUserName) -> claims[JwtStandardClaims.PreferredUserName]!! as String
      hasClaims(PropelAuthJwtTokenClaims.FirstName, PropelAuthJwtTokenClaims.LastName) -> listOf(claims[PropelAuthJwtTokenClaims.FirstName] as String, claims[PropelAuthJwtTokenClaims.LastName] as String)
         .filter { it.isNotEmpty() }
         .joinToString(" ")
      else -> error("Could not infer username from provided claims: $claims")
   }
}

fun Authentication.toVyneUser(): VyneUser {
   return when (this) {
      is JwtAuthenticationToken -> this.toVyneUser()
      else -> TODO("Unhandled authentication type: ${this::class.simpleName}")
   }
}

fun vyneUserFromClaims(claims: Map<String, Any>, authorities: Collection<GrantedAuthority>): VyneUser {
   fun <T> claim(fieldName: String): T? {
      return claims[fieldName] as T?

   }

   fun <T> mandatoryClaim(fieldName: String, fallbackClaim: String? = null): T {
      val claimValue = claim(fieldName) as Any?
         ?: fallbackClaim?.let { claim(it) as Any? }
         ?: error("JWT Token is malformed.  The $fieldName attribute is mandatory, but was not provided also the $fallbackClaim is not available.")
      return claimValue as T
   }

   return VyneUser(
      id = mandatoryClaim(JwtStandardClaims.Sub),
      issuer = mandatoryClaim(JwtStandardClaims.Issuer),
      username = getPreferredUserDisplayName(claims),
      email = mandatoryClaim(JwtStandardClaims.Email, JwtStandardClaims.ClientId),
      profileUrl = claim(JwtStandardClaims.PictureUrl) ?: claim(PropelAuthJwtTokenClaims.PictureUrl),
      name = claim(JwtStandardClaims.Name),
      grantedAuthorities = VyneGrantedAuthority.from(authorities.map { it.authority })
//      isAuthenticated = true
   )
}

fun JwtAuthenticationToken.toVyneUser(): VyneUser {
   val claims = this.token.claims
   return vyneUserFromClaims(claims, this.authorities)

}


/**
 * see https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
 *
 */
object JwtStandardClaims {
   // Subject - Identifier for the End-User at the Issuer.
   const val Sub = "sub"

   // Issuer - the OIDC provider who authenticated the user
   const val Issuer = "iss"

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

   /**
    * Claim included only for api clients (i.e. clients that auhenticates via client secret)
    * PLEASE NOTE THAT This is KeyCloak Specific, other OAuth2 providers will probably don't return this claim.
    * Therefore, revisit the code consuming this claim when there is a need to integrate vyne with other authentication
    * provides.
    */
   const val ClientId = "clientId"
}


/**
 * Jwt claims that are non-standard, and present / needed in the PropelAuth login flow
 */
object PropelAuthJwtTokenClaims {
   const val FirstName = "first_name"
   const val LastName = "last_name"
   const val PictureUrl = "picture_url"
   const val OrgIdToMemberInfo = "org_id_to_org_member_info"
}
