package com.orbitalhq.spring.http.auth

import com.orbitalhq.auth.schemes.AuthTokens
import com.orbitalhq.auth.schemes.OAuth2
import com.orbitalhq.auth.schemes.SimpleAuthSchemeProvider
import com.orbitalhq.spring.http.auth.schemes.OAuth2Utils
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import java.time.Instant

class HttpAuthConfigTest {

   @Test
   fun `oauth refresh tokens are registered in client registry`() {
      val authProvider = SimpleAuthSchemeProvider(AuthTokens(
         mapOf("my-oauth-service" to OAuth2(
            "https://token-api",
            "clientId",
            "clientSecret",
            grantType = OAuth2.AuthorizationGrantType.RefreshToken,
            refreshToken = "my-refresh-token"
         ))
      ))
      val (clientService,clientManager) = oauthAuthorizedClientManager(authProvider)
      val refreshTokenManager = OAuthRefreshTokenManager(clientService, authProvider)
      refreshTokenManager.resetRefreshTokens()
      val authorizedClient = clientService.loadAuthorizedClient<OAuth2AuthorizedClient>("my-oauth-service", OAuth2Utils.ANONYMOUS_USER )
         .block()!!
      authorizedClient.refreshToken.tokenValue.shouldBe("my-refresh-token")
      authorizedClient.accessToken.expiresAt.shouldBeBefore(Instant.now())
   }

   @Test
   fun `oauth client credentials tokens are not registered in client registry`() {
      val authProvider = SimpleAuthSchemeProvider(AuthTokens(
         mapOf("my-oauth-service" to OAuth2(
            "https://token-api",
            "clientId",
            "clientSecret",
            grantType = OAuth2.AuthorizationGrantType.ClientCredentials,
         ))
      ))
      val (clientService,b) = oauthAuthorizedClientManager(authProvider)
      val authorizedClient = clientService.loadAuthorizedClient<OAuth2AuthorizedClient>("my-oauth-service", OAuth2Utils.ANONYMOUS_USER )
         .block()
      authorizedClient.shouldBeNull()
   }
}
