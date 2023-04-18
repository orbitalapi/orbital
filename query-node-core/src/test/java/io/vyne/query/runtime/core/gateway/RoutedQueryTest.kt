package io.vyne.query.runtime.core.gateway

import io.kotest.matchers.shouldBe
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.mock.web.reactive.function.server.MockServerRequest

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
      routedQuery.parameters.entries.single().value.typedValue.value
         .shouldBe("123")
   }

}


fun query(vararg sources: String): Pair<TaxiQlQuery, TaxiQLQueryString> {
   val src = sources.joinToString("\n")
   val schema = TaxiSchema.from(src)
   return schema.taxi.queries.single() to schema.taxi.queries.single().compilationUnits.single().source.content
}
