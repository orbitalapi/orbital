package com.orbitalhq.spring.http.auth.schemes

import io.kotest.matchers.booleans.shouldBeTrue
import com.orbitalhq.auth.schemes.AuthTokens
import com.orbitalhq.auth.schemes.OAuth2
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class AuthWebClientCustomizerTest {
   @Test @Disabled
   fun `adds oauth`() {
      val authScheme = OAuth2(
         "https://accounts.spotify.com/api/token",
         "clientId",
         "clientSecret",
         emptyList(),
         OAuth2.AuthorizationGrantType.ClientCredentials,
         OAuth2.AuthenticationMethod.Basic,
      )

      val serviceName = "MyOAuthService"
      val customizer = AuthWebClientCustomizer.forTokens(
         AuthTokens(mapOf(serviceName to authScheme))
      )
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()
      val responseSpec = webClient.get()
         .uri("https://api.spotify.com/v1/search?q=remaster%2520track%3ADoxy%2520artist%3AMiles%2520Davis&type=album")
         .addAuthTokenAttributes(serviceName)
         .retrieve()
         .toBodilessEntity()
         .block()!!
      responseSpec.statusCode.is2xxSuccessful.shouldBeTrue()
   }
}
