package com.orbitalhq.spring.invokers

import com.orbitalhq.Vyne
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.springframework.web.reactive.function.client.WebClient

class HttpHeadersTest {

   @Rule
   @JvmField
   val server = MockWebServerRule()


   @Test
   fun `http headers are passed`(): Unit = runBlocking {
      val vyne = vyneWithHttpInvoker(
         """
         model Person {
            name : Name inherits String
         }
         type CacheMaxAge inherits Int
         service PeopleService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            operation findPeople(
               @HttpHeader(name = "Cache-Control", prefix = "max-age=") cacheMaxAge : CacheMaxAge
            ):Person[]
         }
      """.trimIndent()
      )
      prepareServerResponse()
      vyne.query(
         """
         given { CacheMaxAge = 9600 }
         find { Person[] } """
      )
         .rawObjects()
      val lastRequest = server.takeRequest()
      lastRequest.getHeader("Cache-Control").shouldBe("max-age=9600")
   }

   @Test
   fun `can mix and match operation headers and param headers`(): Unit = runBlocking {
      val vyne = vyneWithHttpInvoker(
         """
         model Person {
            name : Name inherits String
         }
         type CacheMaxAge inherits Int
         service PeopleService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            @HttpHeader(name = "Consumes", value = "application/json")
            @HttpHeader(name = "Accept-Encoding", value = "gzip")
            operation findPeople(
               @HttpHeader(name = "Cache-Control", prefix = "max-age=") cacheMaxAge : CacheMaxAge
            ):Person[]
         }
      """.trimIndent()
      )
      prepareServerResponse()
      vyne.query(
         """
         given { CacheMaxAge = 9600 }
         find { Person[] } """
      )
         .rawObjects()
      val lastRequest = server.takeRequest()
      lastRequest.getHeader("Cache-Control").shouldBe("max-age=9600")
      lastRequest.getHeader("Consumes").shouldBe("application/json")
      lastRequest.getHeader("Accept-Encoding").shouldBe("gzip")
   }

   private fun prepareServerResponse() {
      server.addJsonResponse("""[ { "name" : "Jimmy" } ]""")
   }

   @Test
   fun `can provide some paramaters headers via expressions`(): Unit = runBlocking {
      // This test exercises where we end up doing a graph search.
      // It's a different code path from where we call DirectServiceInvocation (tested below).
      val vyne = vyneWithHttpInvoker(
         """
         model Person {
            name : Name inherits String
         }
         type CacheMaxAge inherits Int
         service PeopleService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            operation findPeople(
               @HttpHeader(name = "Cache-Control", prefix = "max-age=") cacheMaxAge : CacheMaxAge,
               @HttpHeader(name = "Accept-Encoding") encoding : String = upperCase("gzip")
            ):Person[]
         }
      """.trimIndent()
      )
      prepareServerResponse()
      vyne.query(
         """
         given { CacheMaxAge = 9600 }
         find { Person[] } """
      )
         .rawObjects()
      val lastRequest = server.takeRequest()
      lastRequest.getHeader("Cache-Control").shouldBe("max-age=9600")
      lastRequest.getHeader("Accept-Encoding").shouldBe("GZIP")
   }


   @Test
   fun `can provide default http headers when all values come from expressions`(): Unit = runBlocking {
      // This test examines the situation going through DirectServiceOperationInvoation,
      // which doesn't use a graph search
      val src = """
         model Person {
            name : Name inherits String
         }
         service PeopleService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            operation findPeople(
               @HttpHeader(name = "Accept-Encoding") encoding : String = upperCase("gzip")
            ):Person[]
         }
      """
      val vyne = vyneWithHttpInvoker(src)
      prepareServerResponse()
      vyne.query(
         """
            given { s : String = "" }
            find { Person[] } """.trimIndent()
      )
         .rawObjects()
      val lastRequest = server.takeRequest()
      lastRequest.getHeader("Accept-Encoding").shouldBe("GZIP")
   }
}

fun vyneWithHttpInvoker(src: String): Vyne {
   return testVyne(src) { schema ->
      listOf(
         RestTemplateInvoker(
            SimpleSchemaProvider(schema),
            WebClient.create()
         )
      )
   }
}
