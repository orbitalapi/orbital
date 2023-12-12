package com.orbitalhq.spring.http.auth.schemes

import com.orbitalhq.auth.schemes.OAuth2
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrations
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import java.time.Instant

object OAuth2Utils {
   // The below values were taken by mimicing / debugging what Spring populates.
   // They don't really seem to matter
   val ANONYMOUS_USER = "anonymousUser"
}

fun OAuth2.asClientRegistration(serviceName: String): ClientRegistration {
   return ClientRegistration.withRegistrationId(serviceName)
      .clientId(this.clientId)
      .clientSecret(this.clientSecret)
      .tokenUri(this.accessTokenUrl)
      .clientAuthenticationMethod(this.method.asSpringAuthMethod())
      .authorizationGrantType(this.grantType.asSpringGrantType())
      .build()
}

fun OAuth2.createAuthorizedClient(serviceName: String, tokenValue: String, issuedAt: Instant, expires: Instant):Pair<Authentication,OAuth2AuthorizedClient> {
   val clientRegistration = this.asClientRegistration(serviceName)
   // The below values were taken by mimicing / debugging what Spring populates.
   // They don't really seem to matter
   val principal =
      AnonymousAuthenticationToken(OAuth2Utils.ANONYMOUS_USER, OAuth2Utils.ANONYMOUS_USER, listOf(SimpleGrantedAuthority("ROLE_USER")))
   val client = OAuth2AuthorizedClient(
      clientRegistration,
      principal.name,
      OAuth2AccessToken(
         OAuth2AccessToken.TokenType.BEARER,
         tokenValue,
         issuedAt,
         expires
      ),
      OAuth2RefreshToken(this.refreshToken!!, Instant.now())
   )
   return principal to client
}

private fun OAuth2.AuthorizationGrantType.asSpringGrantType(): AuthorizationGrantType {
   return when (this) {
      OAuth2.AuthorizationGrantType.ClientCredentials -> AuthorizationGrantType.CLIENT_CREDENTIALS
      OAuth2.AuthorizationGrantType.RefreshToken -> AuthorizationGrantType.REFRESH_TOKEN
      OAuth2.AuthorizationGrantType.AuthorizationCode -> AuthorizationGrantType.AUTHORIZATION_CODE
   }

}

private fun OAuth2.AuthenticationMethod.asSpringAuthMethod(): ClientAuthenticationMethod {
   return when (this) {
      OAuth2.AuthenticationMethod.Basic -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC
      OAuth2.AuthenticationMethod.JWT -> ClientAuthenticationMethod.CLIENT_SECRET_JWT
      OAuth2.AuthenticationMethod.Post -> ClientAuthenticationMethod.CLIENT_SECRET_POST
   }

}
