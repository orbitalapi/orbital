package com.orbitalhq.query.runtime.core.gateway

import io.kotest.matchers.shouldBe
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.withBuiltIns
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import org.junit.Test
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import reactor.core.publisher.Mono

class RoutedQueryTest {
   val src = """
      model Film {
         filmId : FilmId inherits String
      }
   """.trimIndent()

   @Test
   fun `converts path variables to facts`() {
      val (query, querySrc) = query(
         src, """

         @HttpOperation(method = "GET", url = "/films/{filmId}")
         query findFilm( @PathVariable("filmId") filmId : FilmId ) {
            find { Film( FilmId == filmId ) }
         }
      """.trimIndent()
      )

      val request = MockServerRequest.builder()
         .pathVariable("filmId", "123")
         .build()

      val routedQuery = RoutedQuery.build(query, querySrc, request)
      routedQuery.block().arguments.entries.single().value.typedValue.value
         .shouldBe("123")
   }

   @Test
   fun `converts request body to facts`() {
      val (query, querySrc) = query(
         src, """

         @HttpOperation(method = "GET", url = "/films/{filmId}")
         query findFilm( @RequestBody film : Film ) {
            find { Film }
         }
      """.trimIndent()
      )

      val requestBody = """{ "id" : 123 }"""
      val request = MockServerRequest.builder()
         .body(Mono.just(requestBody))

      val routedQuery = RoutedQuery.build(query, querySrc, request)
      routedQuery.block().arguments.entries.single().value.typedValue.value
         .shouldBe(requestBody)
   }

}


fun query(vararg sources: String): Pair<TaxiQlQuery, TaxiQLQueryString> {
   val src = sources.joinToString("\n")
   val schema = TaxiSchema.from(src).withBuiltIns()
   return schema.taxi.queries.single() to schema.taxi.queries.single().compilationUnits.single().source.content
}
