package io.vyne.spring.http.auth.schemes

import io.kotest.matchers.booleans.shouldBeTrue
import io.vyne.auth.schemes.AuthTokens
import io.vyne.auth.schemes.OAuth2
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class AuthWebClientCustomizerTest {
   @Test
   fun `adds oauth`() {
      val authScheme = OAuth2(
         "https://accounts.spotify.com/api/token",
         "8ac3acccbe204952ae69c910b2952b48",
         "13ac7256e7f5483dbed949597699cc82",
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
