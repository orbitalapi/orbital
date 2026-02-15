package com.orbitalhq.spring.http.auth.schemes

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.orbitalhq.auth.schemes.*
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

@WireMockTest
class MultiAuthWebClientCustomizerTest {

   @Test
   fun `applies multiple auth schemes to same request`(wiremockInfo: WireMockRuntimeInfo) {
      val host = "http://localhost:${wiremockInfo.httpPort}"
      val serviceName = "com.foo.TestService"

      stubFor(
         get(urlPathEqualTo("/api/resource"))
            .willReturn(
               aResponse()
                  .withStatus(200)
                  .withBody("success")
            )
      )

      val authTokens = AuthTokens(
         mapOf(
            serviceName to listOf(
               HttpHeader(value = "bearer-token-123"),
               QueryParam(parameterName = "apiKey", value = "key-456"),
               Cookie(cookieName = "session", value = "session-789")
            )
         )
      )

      val customizer = AuthWebClientCustomizer.forTokens(authTokens)
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()

      val response = webClient.get()
         .uri("$host/api/resource")
         .addAuthTokenAttributes(serviceName)
         .retrieve()
         .toBodilessEntity()
         .block()!!

      response.statusCode.is2xxSuccessful.shouldBeTrue()

      // Verify all auth mechanisms were applied
      verify(
         getRequestedFor(urlPathEqualTo("/api/resource"))
            .withHeader("Authorization", equalTo("Bearer bearer-token-123"))
            .withQueryParam("apiKey", equalTo("key-456"))
            .withCookie("session", equalTo("session-789"))
      )
   }

   @Test
   fun `applies multiple headers with different names`(wiremockInfo: WireMockRuntimeInfo) {
      val host = "http://localhost:${wiremockInfo.httpPort}"
      val serviceName = "com.foo.TestService"

      stubFor(
         get(urlPathEqualTo("/api/resource"))
            .willReturn(
               aResponse()
                  .withStatus(200)
                  .withBody("success")
            )
      )

      val authTokens = AuthTokens(
         mapOf(
            serviceName to listOf(
               HttpHeader(value = "token1", headerName = "X-API-Token"),
               HttpHeader(value = "token2", prefix = "", headerName = "X-Client-ID")
            )
         )
      )

      val customizer = AuthWebClientCustomizer.forTokens(authTokens)
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()

      val response = webClient.get()
         .uri("$host/api/resource")
         .addAuthTokenAttributes(serviceName)
         .retrieve()
         .toBodilessEntity()
         .block()!!

      response.statusCode.is2xxSuccessful.shouldBeTrue()

      verify(
         getRequestedFor(urlPathEqualTo("/api/resource"))
            .withHeader("X-API-Token", equalTo("Bearer token1"))
            .withHeader("X-Client-ID", equalTo("token2"))
      )
   }

   @Test
   fun `backwards compatibility - single scheme still works`(wiremockInfo: WireMockRuntimeInfo) {
      val host = "http://localhost:${wiremockInfo.httpPort}"
      val serviceName = "com.foo.TestService"

      stubFor(
         get(urlPathEqualTo("/api/resource"))
            .willReturn(
               aResponse()
                  .withStatus(200)
                  .withBody("success")
            )
      )

      // Old format: single scheme wrapped in list internally
      val authTokens = AuthTokens(
         mapOf(
            serviceName to listOf(
               HttpHeader(value = "bearer-token-123")
            )
         )
      )

      val customizer = AuthWebClientCustomizer.forTokens(authTokens)
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()

      val response = webClient.get()
         .uri("$host/api/resource")
         .addAuthTokenAttributes(serviceName)
         .retrieve()
         .toBodilessEntity()
         .block()!!

      response.statusCode.is2xxSuccessful.shouldBeTrue()

      verify(
         getRequestedFor(urlPathEqualTo("/api/resource"))
            .withHeader("Authorization", equalTo("Bearer bearer-token-123"))
      )
   }

   @Test
   fun `applies query params and headers together`(wiremockInfo: WireMockRuntimeInfo) {
      val host = "http://localhost:${wiremockInfo.httpPort}"
      val serviceName = "com.foo.TestService"

      stubFor(
         get(urlPathEqualTo("/api/data"))
            .willReturn(
               aResponse()
                  .withStatus(200)
                  .withBody("{\"result\": \"ok\"}")
            )
      )

      val authTokens = AuthTokens(
         mapOf(
            serviceName to listOf(
               QueryParam(parameterName = "token", value = "abc123"),
               QueryParam(parameterName = "client_id", value = "myClient"),
               HttpHeader(value = "secret-key", headerName = "X-Secret")
            )
         )
      )

      val customizer = AuthWebClientCustomizer.forTokens(authTokens)
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()

      val response = webClient.get()
         .uri("$host/api/data")
         .addAuthTokenAttributes(serviceName)
         .retrieve()
         .toBodilessEntity()
         .block()!!

      response.statusCode.is2xxSuccessful.shouldBeTrue()

      verify(
         getRequestedFor(urlPathEqualTo("/api/data"))
            .withQueryParam("token", equalTo("abc123"))
            .withQueryParam("client_id", equalTo("myClient"))
            .withHeader("X-Secret", equalTo("Bearer secret-key"))
      )
   }

   @Test
   fun `empty list returns no auth applied`(wiremockInfo: WireMockRuntimeInfo) {
      val host = "http://localhost:${wiremockInfo.httpPort}"
      val serviceName = "com.foo.TestService"

      stubFor(
         get(urlPathEqualTo("/api/public"))
            .willReturn(
               aResponse()
                  .withStatus(200)
                  .withBody("public resource")
            )
      )

      // Empty auth configuration
      val authTokens = AuthTokens(emptyMap())

      val customizer = AuthWebClientCustomizer.forTokens(authTokens)
      val webClient = WebClient.builder()
         .filter(customizer.authFromServiceNameAttribute)
         .build()

      val response = webClient.get()
         .uri("$host/api/public")
         .addAuthTokenAttributes(serviceName)
         .retrieve()
         .toBodilessEntity()
         .block()!!

      response.statusCode.is2xxSuccessful.shouldBeTrue()

      // Verify no auth headers or params were added
      verify(
         getRequestedFor(urlPathEqualTo("/api/public"))
            .withoutHeader("Authorization")
      )
   }
}
