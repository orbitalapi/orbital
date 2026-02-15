package com.orbitalhq.spring.http.auth.schemes

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.orbitalhq.auth.schemes.AuthTokens
import com.orbitalhq.auth.schemes.OAuth2
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.web.reactive.function.client.WebClient

@WireMockTest
class OAuthWebClientCustomizerTest {

   @Test
   fun `adds oauth`(wiremockInfo: WireMockRuntimeInfo) {

      val host = "http://localhost:${wiremockInfo.httpPort}"

      stubFor(
         post("/api/token")
            .willReturn(
               okJson(
                  """{
    "access_token": "the-access-token",
    "token_type": "Bearer",
    "expires_in": 3600
}"""
               )
            )
      )
      stubFor(get("/some-protected-resource").willReturn(ok()))
      // originally tested against spotify with the following url:
      // https://accounts.spotify.com/api/token
      val authScheme = OAuth2(
         "$host/api/token",
         "myClientId",
         "myClientSecret",
         emptyList(),
         OAuth2.AuthorizationGrantType.ClientCredentials,
         OAuth2.AuthenticationMethod.Basic,
      )

      val serviceName = "MyOAuthService"
      val customizer = AuthWebClientCustomizer.forTokens(
         AuthTokens(mapOf(serviceName to listOf(authScheme))),
         oneTimeRefreshTokenReset = true
      )
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()

      // Originally built / tested against Spotify, using the following URL as the get:
      // https://api.spotify.com/v1/search?q=remaster%2520track%3ADoxy%2520artist%3AMiles%2520Davis&type=album
      val responseSpec = webClient.get()
         .uri("$host/some-protected-resource")
         .addAuthTokenAttributes(serviceName)
         .retrieve()
         .toBodilessEntity()
         .block()!!

      responseSpec.statusCode.is2xxSuccessful.shouldBeTrue()
      verify(
         getRequestedFor(urlEqualTo("/some-protected-resource"))
            .withHeader("Authorization", equalTo("Bearer the-access-token"))
      )
   }

   @Test
   fun `adds oauth when registered with wildcard`(wiremockInfo: WireMockRuntimeInfo) {

      val host = "http://localhost:${wiremockInfo.httpPort}"

      stubFor(
         post("/api/token")
            .willReturn(
               okJson(
                  """{
    "access_token": "the-access-token",
    "token_type": "Bearer",
    "expires_in": 3600
}"""
               )
            )
      )
      stubFor(get("/some-protected-resource").willReturn(ok()))
      // originally tested against spotify with the following url:
      // https://accounts.spotify.com/api/token
      val authScheme = OAuth2(
         "$host/api/token",
         "myClientId",
         "myClientSecret",
         emptyList(),
         OAuth2.AuthorizationGrantType.ClientCredentials,
         OAuth2.AuthenticationMethod.Basic,
      )

      val serviceName = "com.foo.MyOAuthService"
      val customizer = AuthWebClientCustomizer.forTokens(
         AuthTokens(mapOf("com.foo.*" to listOf(authScheme))),
         oneTimeRefreshTokenReset = true
      )
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()

      // Originally built / tested against Spotify, using the following URL as the get:
      // https://api.spotify.com/v1/search?q=remaster%2520track%3ADoxy%2520artist%3AMiles%2520Davis&type=album
      val responseSpec = webClient.get()
         .uri("$host/some-protected-resource")
         .addAuthTokenAttributes(serviceName)
         .retrieve()
         .toBodilessEntity()
         .block()!!

      responseSpec.statusCode.is2xxSuccessful.shouldBeTrue()
      verify(
         getRequestedFor(urlEqualTo("/some-protected-resource"))
            .withHeader("Authorization", equalTo("Bearer the-access-token"))
      )
   }


   @Test
   fun `adds oauth using refresh token`(wiremockInfo: WireMockRuntimeInfo) {
      val host = "http://localhost:${wiremockInfo.httpPort}"

      // originally built/tested against Zoho API:
      // https://accounts.zoho.eu/oauth/v2/token
      val authScheme = OAuth2(
         "$host/oauth/v2/token",
         "my-client-id",
         "my-client-secret",
         emptyList(),
         OAuth2.AuthorizationGrantType.RefreshToken,
         OAuth2.AuthenticationMethod.Post,
         "my-refresh-token"
      )

      stubFor(
         post(urlPathEqualTo("/oauth/v2/token")).withQueryParams(
            mapOf(
               "refresh_token" to equalTo("my-refresh-token"),
               "client_id" to equalTo("my-client-id"),
               "client_secret" to equalTo("my-client-secret"),
               "grant_type" to equalTo("refresh_token")
            )
         ).willReturn(
            // Note: api_domain below is not part of the spec, but included in responses from Zoho etc.
            // Including it here to verify that deserialization doesn't barf for unknown properties
            okJson(
               """
         {
             "access_token": "my-access-token",
             "api_domain": "https://www.somedomain.eu",
             "token_type": "Bearer",
             "expires_in": 3600
         }
      """.trimIndent()
            )
         )
      )

      stubFor(get(urlPathEqualTo("/my-protected-resource")).willReturn(ok()))

      val serviceName = "MyOAuthService"
      val customizer = AuthWebClientCustomizer.forTokens(
         AuthTokens(mapOf(serviceName to listOf(authScheme))),
         oneTimeRefreshTokenReset = true
      )
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()

      // Send multiple requests. We should only request the OAuth token the first time
      repeat(2) {
         val responseSpec = webClient.get()
            // Originally built against Zoho with the following get url:
            // https://www.zohoapis.eu/crm/v5/Deals?fields=Deal_Name,Stage,Account_Name,Amount,Owner,Lead_Source,Type,Last_Activity_Time
            .uri("$host/my-protected-resource")
            .addAuthTokenAttributes(serviceName)
            .retrieve()
            .toBodilessEntity()
            .block()!!
         responseSpec.statusCode.is2xxSuccessful.shouldBeTrue()
      }

      verify(
         2,
         getRequestedFor(urlPathEqualTo("/my-protected-resource"))
            .withHeader("Authorization", equalTo("Bearer my-access-token"))
      )

      // Even though we access the protected resource twice, we should only have requested a token once.
      verify(1, postRequestedFor(urlPathEqualTo("/oauth/v2/token")))
   }

   @Test
   fun `adds oauth using refresh token when reigstered with wildcard`(wiremockInfo: WireMockRuntimeInfo) {
      val host = "http://localhost:${wiremockInfo.httpPort}"

      // originally built/tested against Zoho API:
      // https://accounts.zoho.eu/oauth/v2/token
      val authScheme = OAuth2(
         "$host/oauth/v2/token",
         "my-client-id",
         "my-client-secret",
         emptyList(),
         OAuth2.AuthorizationGrantType.RefreshToken,
         OAuth2.AuthenticationMethod.Post,
         "my-refresh-token"
      )

      stubFor(
         post(urlPathEqualTo("/oauth/v2/token")).withQueryParams(
            mapOf(
               "refresh_token" to equalTo("my-refresh-token"),
               "client_id" to equalTo("my-client-id"),
               "client_secret" to equalTo("my-client-secret"),
               "grant_type" to equalTo("refresh_token")
            )
         ).willReturn(
            // Note: api_domain below is not part of the spec, but included in responses from Zoho etc.
            // Including it here to verify that deserialization doesn't barf for unknown properties
            okJson(
               """
         {
             "access_token": "my-access-token",
             "api_domain": "https://www.somedomain.eu",
             "token_type": "Bearer",
             "expires_in": 3600
         }
      """.trimIndent()
            )
         )
      )

      stubFor(get(urlPathEqualTo("/my-protected-resource")).willReturn(ok()))

      val serviceName = "com.foo.MyOAuthService"
      val customizer = AuthWebClientCustomizer.forTokens(
         AuthTokens(mapOf("com.foo.*" to listOf(authScheme))),
         oneTimeRefreshTokenReset = true
      )
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()

      val responseSpec = webClient.get()
         // Originally built against Zoho with the following get url:
         // https://www.zohoapis.eu/crm/v5/Deals?fields=Deal_Name,Stage,Account_Name,Amount,Owner,Lead_Source,Type,Last_Activity_Time
         .uri("$host/my-protected-resource")
         .addAuthTokenAttributes(serviceName)
         .retrieve()
         .toBodilessEntity()
         .block()!!
      responseSpec.statusCode.is2xxSuccessful.shouldBeTrue()

      verify(
         1,
         getRequestedFor(urlPathEqualTo("/my-protected-resource"))
            .withHeader("Authorization", equalTo("Bearer my-access-token"))
      )
   }

   @Test
   fun `when oauth token refresh fails with 200 error is propogated `(wiremockInfo: WireMockRuntimeInfo) {
      val host = "http://localhost:${wiremockInfo.httpPort}"

      // originally built/tested against Zoho API:
      // https://accounts.zoho.eu/oauth/v2/token
      val authScheme = OAuth2(
         "$host/oauth/v2/token",
         "my-client-id",
         "my-client-secret",
         emptyList(),
         OAuth2.AuthorizationGrantType.RefreshToken,
         OAuth2.AuthenticationMethod.Post,
         "my-refresh-token"
      )

      stubFor(
         post(urlPathEqualTo("/oauth/v2/token")).withQueryParams(
            mapOf(
               "refresh_token" to equalTo("my-refresh-token"),
               "client_id" to equalTo("my-client-id"),
               "client_secret" to equalTo("my-client-secret"),
               "grant_type" to equalTo("refresh_token")
            )
         ).willReturn(
            // Zoho will respond with a 200 OK response and an error payload.
            okJson(
               """
         {
             "error": "invalid_code"
         }
      """.trimIndent()
            )
         )
      )

      stubFor(get(urlPathEqualTo("/my-protected-resource")).willReturn(ok()))

      val serviceName = "MyOAuthService"
      val customizer = AuthWebClientCustomizer.forTokens(
         AuthTokens(mapOf(serviceName to listOf(authScheme))),
         oneTimeRefreshTokenReset = true
      )
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()

      val exception = assertThrows<OAuth2AuthenticationException> {
         webClient.get()
            .uri("$host/my-protected-resource")
            .addAuthTokenAttributes(serviceName)
            .retrieve()
            .toBodilessEntity()
            .block()!!
      }
      exception.error.errorCode.shouldBe("invalid_code")
   }
}
