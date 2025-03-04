package com.orbitalhq.spring.invokers

import com.orbitalhq.firstRawObject
import com.orbitalhq.http.MockWebServerRule
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test

class RestTemplateInvokerPathVariablesTest {


   @Rule
   @JvmField
   val server = MockWebServerRule()


   @Test
   fun `PathVariable matches on value of annotation`():Unit = runBlocking {
      val vyne = testVyne("""
         model Person {
            id : PersonId inherits String
            name : Name inherits String
         }
         service PersonApi {
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/person/{id}")
            operation getPerson(@PathVariable("id") personId:PersonId):Person
         }
      """.trimIndent(),
         Invoker.RestTemplate)
      server.addJsonResponse("""{ "id" : "123", "name" : "Jimmy" }""")
      vyne.query("""
         given { PersonId = "123" }
         find { Person }
      """.trimIndent())
         .firstRawObject().shouldBe(mapOf("id" to "123", "name" to "Jimmy"))

      val request = server.takeRequest()
      request.path.shouldBe("/person/123")
   }

   @Test
   fun `PathVariable matches on value of annotation when variable doesn't have name`():Unit = runBlocking {
      val vyne = testVyne("""
         model Person {
            id : PersonId inherits String
            name : Name inherits String
         }
         service PersonApi {
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/person/{id}")
            // Note: No variable name for PersonId
            operation getPerson(@PathVariable("id") PersonId):Person
         }
      """.trimIndent(),
         Invoker.RestTemplate)
      server.addJsonResponse("""{ "id" : "123", "name" : "Jimmy" }""")
      vyne.query("""
         given { PersonId = "123" }
         find { Person }
      """.trimIndent())
         .firstRawObject().shouldBe(mapOf("id" to "123", "name" to "Jimmy"))

      val request = server.takeRequest()
      request.path.shouldBe("/person/123")
   }

   // https://github.com/orbitalapi/orbital/issues/15
   @Test
   fun `can use a path variable with dots`():Unit = runBlocking {
      val vyne = testVyne("""
         model Person {
            id : PersonId inherits String
            name : Name inherits String
         }
         service PersonApi {
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/person/{person.id}")
            operation getPerson(@PathVariable("person.id") personId:PersonId):Person
         }
      """.trimIndent(),
         Invoker.RestTemplate)
      server.addJsonResponse("""{ "id" : "123", "name" : "Jimmy" }""")
      vyne.query("""
         given { PersonId = "123" }
         find { Person }
      """.trimIndent())
         .firstRawObject().shouldBe(mapOf("id" to "123", "name" to "Jimmy"))

      val request = server.takeRequest()
      request.path.shouldBe("/person/123")
   }

   // https://github.com/orbitalapi/orbital/issues/15
   @Test
   fun `can use a query variable with dots`():Unit = runBlocking {
      val vyne = testVyne("""
         model Person {
            id : PersonId inherits String
            name : Name inherits String
         }
         service PersonApi {
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/person")
            operation getPerson(@taxi.http.QueryVariable("person.id") personId:PersonId):Person
         }
      """.trimIndent(),
         Invoker.RestTemplate)
      server.addJsonResponse("""{ "id" : "123", "name" : "Jimmy" }""")
      vyne.query("""
         given { PersonId = "123" }
         find { Person }
      """.trimIndent())
         .firstRawObject().shouldBe(mapOf("id" to "123", "name" to "Jimmy"))

      val request = server.takeRequest()
      request.path.shouldBe("/person?person.id=123")
   }

   @Test
   fun `query variable matches on annotaion name when parameter doesn't have name`():Unit = runBlocking {
      val vyne = testVyne("""
         model Person {
            id : PersonId inherits String
            name : Name inherits String
         }
         service PersonApi {
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/person")
            operation getPerson(@taxi.http.QueryVariable("person.id") PersonId):Person
         }
      """.trimIndent(),
         Invoker.RestTemplate)
      server.addJsonResponse("""{ "id" : "123", "name" : "Jimmy" }""")
      vyne.query("""
         given { PersonId = "123" }
         find { Person }
      """.trimIndent())
         .firstRawObject().shouldBe(mapOf("id" to "123", "name" to "Jimmy"))

      val request = server.takeRequest()
      request.path.shouldBe("/person?person.id=123")
   }
}
