package com.orbitalhq.spring.invokers

import com.orbitalhq.Vyne
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.spring.http.auth.schemes.AuthWebClientCustomizer
import com.orbitalhq.testVyne
import com.winterbe.expekt.should
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HttpHttpHeaderNamesTest {

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
   fun `can use a date expression on modified since`():Unit = runBlocking {
      val vyne = vyneWithHttpInvoker(
         """
         model Person {
            name : Name inherits String
         }
         service PeopleService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            operation findPeople(
               @HttpHeader(name = "If-Modified-Since") ifModifiedSince : Instant = addDays(now(), -1)
            ):Person[]
         }
      """.trimIndent()
      )
      prepareServerResponse()
      vyne.query(
         """
         find { Person[] } """
      )
         .rawObjects()
      val lastRequest = server.takeRequest()
      val modifiedSinceHeader = lastRequest.getHeader("If-Modified-Since")

      modifiedSinceHeader.shouldNotBeNull()
      val parsedInstant = Instant.parse(modifiedSinceHeader)
      // if that parsed, then the test is ok
   }

   @Test
   fun `can use a formatted type expression on modified since`():Unit = runBlocking {
      val dateFormat = "dd/MMM/yyyy HH:mm:ss"
      val vyne = vyneWithHttpInvoker(
         """
         @Format("$dateFormat")
         type CustomModifiedSinceHeader inherits Instant
         model Person {
            name : Name inherits String
         }
         service PeopleService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            operation findPeople(
               @HttpHeader(name = "If-Modified-Since") ifModifiedSince : CustomModifiedSinceHeader = (CustomModifiedSinceHeader) addDays(now(), -1)
            ):Person[]
         }
      """.trimIndent()
      )
      prepareServerResponse()
      vyne.query(
         """
         find { Person[] } """
      )
         .rawObjects()
      val lastRequest = server.takeRequest()
      val modifiedSinceHeader = lastRequest.getHeader("If-Modified-Since")

      modifiedSinceHeader.shouldNotBeNull()
      val formatter = DateTimeFormatter.ofPattern(dateFormat)
      val parsedResult =  LocalDateTime.parse(modifiedSinceHeader, formatter)
      // if that parsed, then the test is ok
   } @Test
   fun `can use a formatted type expression on modified since with standard format`():Unit = runBlocking {
      val dateFormat = "yyyy-MM-dd'T'HH:mm:ssX"
      val vyne = vyneWithHttpInvoker(
         """
         @Format("$dateFormat")
         type CustomModifiedSinceHeader inherits Instant
         model Person {
            name : Name inherits String
         }
         service PeopleService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            operation findPeople(
               @HttpHeader(name = "If-Modified-Since") ifModifiedSince : CustomModifiedSinceHeader = (CustomModifiedSinceHeader) addDays(now(), -1)
            ):Person[]
         }
      """.trimIndent()
      )
      prepareServerResponse()
      vyne.query(
         """
         find { Person[] } """
      )
         .rawObjects()
      val lastRequest = server.takeRequest()
      val modifiedSinceHeader = lastRequest.getHeader("If-Modified-Since")

      modifiedSinceHeader.shouldNotBeNull()
      val formatter = DateTimeFormatter.ofPattern(dateFormat)
      val parsedResult =  LocalDateTime.parse(modifiedSinceHeader, formatter)
      // if that parsed, then the test is ok
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

    @Test
    fun `when accept is not specified for http operation default accept values are passed to request`(): Unit = runBlocking {
        val src = """
         model Person {
            name : Name inherits String
         }
         service PeopleService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            operation findPeople():Person[]
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
        lastRequest.getHeader("Accept").should.equal(RestTemplateInvoker.defaultAcceptHeaderValue.joinToString { "$it" })
    }

    @Test
    fun `when accept is specified for http operation default accept values are not passed to request`(): Unit = runBlocking {
        val src = """
         model Person {
            name : Name inherits String
         }
         service PeopleService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            @HttpHeader(name = "Accept", value = "application/json")
            operation findPeople():Person[]
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
        lastRequest.getHeader("Accept").should.equal("application/json")
    }
}

fun vyneWithHttpInvoker(src: String): Vyne {
   return testVyne(src) { schema ->
      listOf(
         RestTemplateInvoker(
            SimpleSchemaProvider(schema),
            webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
         )
      )
   }
}
