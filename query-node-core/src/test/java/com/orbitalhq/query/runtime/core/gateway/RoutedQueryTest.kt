package com.orbitalhq.query.runtime.core.gateway

import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.http.HttpStatusException
import com.orbitalhq.withBuiltIns
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import reactor.core.publisher.Mono

class RoutedQueryTest {
   val src = """
      model Film {
         filmId : FilmId inherits String
      }

      type CorrelationId inherits String
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

      val routedQuery = RoutedQuery.build(query, request)
      routedQuery.block()!!.arguments.entries.single().value.typedValue.value
         .shouldBe("123")
   }

    @Test
    fun `converts query variables to facts`() {
        val (query, querySrc) = query(
            src, """

         @HttpOperation(method = "GET", url = "/films")
         query findFilm( @taxi.http.QueryVariable("filmId") filmId : FilmId ) {
            find { Film( FilmId == filmId ) }
         }
      """.trimIndent()
        )

        val request = MockServerRequest.builder()
            .queryParam("filmId", "123")
            .build()

        val routedQuery = RoutedQuery.build(query,  request)
        routedQuery.block()!!.arguments.entries.single().value.typedValue.value
            .shouldBe("123")
    }

    @Test
    fun `converts http header variables to facts`() {
        val (query, querySrc) = query(
            src, """

         @HttpOperation(method = "GET", url = "/films/{filmId}")
         query findFilm( @PathVariable("filmId") filmId : FilmId, @taxi.http.HttpHeader(name = "x-api-request-id" ) correlationId: CorrelationId) {
            find { Film( FilmId == filmId ) }
         }
      """.trimIndent()
        )

        val request = MockServerRequest.builder()
            .pathVariable("filmId", "123")
            .header("x-api-request-id", "request-365")
            .build()

        val routedQuery = RoutedQuery.build(query, request)
        routedQuery.block()!!.arguments.entries.toList()[0].value.typedValue.value
            .shouldBe("123")

        routedQuery.block()!!.arguments.entries.toList()[1].value.typedValue.value
            .shouldBe("request-365")
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

      val routedQuery = RoutedQuery.build(query, request)
      routedQuery.block()!!.arguments.entries.single().value.typedValue.value
         .shouldBe(requestBody)
   }

   @Test
   fun `fails with bad request if request body is missing`() {
      val (query, querySrc) = query(
         src, """

         @HttpOperation(method = "POST", url = "/films/{filmId}")
         query findFilm( @RequestBody film : Film ) {
            find { Film }
         }
      """.trimIndent()
      )
      val request = MockServerRequest.builder()
         .body(Mono.empty<String>())

      val routedQuery = RoutedQuery.build(query, request)
      val exception = assertThrows<HttpStatusException> { routedQuery.block() }
      exception.status.shouldBe(HttpStatus.BAD_REQUEST)
      exception.message.shouldBe("Expected a request body, but none was provided")
   }

   @Test
   fun `fails with bad request if request parameter is missing`() {
      val (query, querySrc) = query(
         src, """

         @HttpOperation(method = "GET", url = "/films/{filmId}")
         query findFilm( @PathVariable("filmId") filmId : FilmId ) {
            find { Film }
         }
      """.trimIndent()
      )
      val request = MockServerRequest.builder()
         .body(Mono.empty<String>())

      val routedQuery = RoutedQuery.build(query, request)
      val exception = assertThrows<HttpStatusException> { routedQuery.block() }
      exception.status.shouldBe(HttpStatus.BAD_REQUEST)
      exception.message.shouldBe("""No path variable with name "filmId" available""")
   }

   @Test
   fun `fails with bad request if header parameter is missing`() {
      val (query, querySrc) = query(
         src, """

         @HttpOperation(method = "GET", url = "/films")
         query findFilm( @taxi.http.HttpHeader(name = "x-film-id") filmId : FilmId ) {
            find { Film }
         }
      """.trimIndent()
      )
      val request = MockServerRequest.builder()
         .body(Mono.empty<String>())

      val routedQuery = RoutedQuery.build(query, request)
      val exception = assertThrows<HttpStatusException> { routedQuery.block() }
      exception.status.shouldBe(HttpStatus.BAD_REQUEST)
      exception.message.shouldBe("""HTTP header "x-film-id" was not provided, and is required""")
   }

   @Test
   fun `fails with bad request if mandatory queryVariable parameter is missing`() {
      val (query, querySrc) = query(
         src, """
         @HttpOperation(method = "GET", url = "/calculate")
         query calculate( @taxi.http.QueryVariable("x-version") version : Int ) {
            find { 1 + 2 }
         }
      """.trimIndent()
      )
      val request = MockServerRequest.builder()
         .body(Mono.empty<String>())

      val routedQuery = RoutedQuery.build(query, request)
      val exception = assertThrows<HttpStatusException> { routedQuery.block() }
      exception.status.shouldBe(HttpStatus.BAD_REQUEST)
      exception.message.shouldBe("""query variable "x-version" was not provided, and is required""")
   }

   @Test
   fun `returns ok if if optional queryVariable parameter is missing`() {
      val (query, querySrc) = query(
         src, """
         @HttpOperation(method = "GET", url = "/calculate")
         query calculate( @taxi.http.QueryVariable("x-otherValue") otherValue : Int? ) {
            find { 1 + (Int) coalesce(otherValue, 2) }
         }
      """.trimIndent()
      )
      val request = MockServerRequest.builder()
         .body(Mono.empty<String>())

      val routedQuery = RoutedQuery.build(query, request).block()!!
      // Null is not provided as a value here, instead we treat the values as "not provided"
      routedQuery.argumentValues.shouldBeEmpty()
   }

}


fun query(vararg sources: String): Pair<TaxiQlQuery, TaxiQLQueryString> {
   val src = sources.joinToString("\n")
   val schema = TaxiSchema.from(src).withBuiltIns()
   return schema.taxi.queries.single() to schema.taxi.queries.single().compilationUnits.single().source.content
}
