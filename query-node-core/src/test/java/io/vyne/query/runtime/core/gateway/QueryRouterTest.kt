package io.vyne.query.runtime.core.gateway

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.http.HttpMethod
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.server.ServerWebExchange
import java.net.URI

class QueryRouterTest {

   val src = """
      model Film {
         filmId : FilmId inherits String
      }
   """.trimIndent()

   @Test
   fun `matches query with path variable`() {
      val query = query(
         src,
         """
         @HttpOperation(method = "GET", url = "/films/{filmId}")
         query findFilm( @PathVariable("filmId") filmId : FilmId ) {
            find { Film( FilmId == filmId ) }
         }
      """.trimIndent()
      ).first
      val router = QueryRouter.build(listOf(query))

      router.getQuery(get("/films")).shouldBeNull()
      router.getQuery(get("/foo")).shouldBeNull()
      router.getQuery(get("/films/123/456")).shouldBeNull()
      router.getQuery(post("/films/123")).shouldBeNull()

      router.getQuery(get("/films/123"))!!.name.fullyQualifiedName.shouldBe("findFilm")
   }


   private fun post(path: String):ServerRequest {
      return MockServerRequest.builder()
         .uri(URI.create(path))
         .method(HttpMethod.POST)
         .build()
   }
   private fun get(path: String): ServerRequest {
      return MockServerRequest.builder()
         .uri(URI.create(path))
         .method(HttpMethod.GET)
         .build()
   }
}
