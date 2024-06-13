package com.orbitalhq.cockpit.core.content

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.http.response
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import reactor.core.publisher.Mono
import reactor.kotlin.test.test

class HttpContentRepositoryTest {

   @Rule
   @JvmField
   final val mockWebServer = MockWebServerRule()

   lateinit var repo: HttpContentRepository

   @Before
   fun setup() {
      repo = HttpContentRepository(
         ContentSettings(url = mockWebServer.url("/").toString())
      )
   }

   val request = ContentRequest("test", "location")

   @Test
   fun `should return content when HTTP call is successful`() {
      val responseBody = listOf(cardData)
      mockWebServer.addJsonResponse(jacksonObjectMapper().writeValueAsString(responseBody))

      repo.loadContent(request)
         .test()
         .expectNextMatches { next -> next == responseBody }
         .verifyComplete()
   }

   @Test
   fun `should return empty when HTTP call fails`() {
      mockWebServer.prepareResponse { response ->
         response.setResponseCode(500)
      }

      repo.loadContent(request)
         .test()
         // verifyComplete() is how we verify that the response is empty
         .verifyComplete()
   }

}
