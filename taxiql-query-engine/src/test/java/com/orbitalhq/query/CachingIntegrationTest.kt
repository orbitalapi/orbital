package com.orbitalhq.query

import com.orbitalhq.Vyne
import com.orbitalhq.query.caching.CacheAnnotation
import com.orbitalhq.query.connectors.CacheAwareOperationInvocationDecorator
import com.orbitalhq.query.graph.operationInvocation.cache.OperationCacheFactory
import com.orbitalhq.rawObjects
import com.orbitalhq.schemas.QueryScopedCache
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyne
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class CachingIntegrationTest {

   @Test
   fun `default behaviour caches operation calls within a query`(): Unit = runBlocking {
      val (vyne, stub) = vyneWithCachingStub(
         """
         model Person {
            id : PersonId inherits Int
            accountId : AccountId inherits Int
            name : PersonName inherits String
         }
         model AccountState {
            balance : AccountBalance inherits Decimal
         }
         service PersonApi {
            operation getPeople():Person[]
            operation getAccount(AccountId):AccountState
         }
      """.trimIndent()
      )
      stub.addResponse(
         "getPeople", """[
         |{"id" : 1, "accountId" : 100, "name" : "Jim" },
         |{"id" : 2, "accountId" : 100, "name" : "Jill" }
         |]""".trimMargin()
      )
      stub.addResponse("getAccount", """{ "balance" : 555.00 }""")
      val result = vyne.query(
         """find { Person[] } as {
         |  name : PersonName
         |  balance: AccountBalance
         |}[]
      """.trimMargin()
      ).rawObjects()
      result.shouldBe(
         listOf(
            mapOf("name" to "Jim", "balance" to 555.00.toBigDecimal()),
            mapOf("name" to "Jill", "balance" to 555.00.toBigDecimal())
         )
      )

      // balance service should've been only once,
      // as caching the operation call is cached
      stub.calls["getAccount"].shouldHaveSize(1)
   }

   @Test
   fun `an operation tagged as do not cache is never cached`(): Unit = runBlocking {
      val (vyne, stub) = vyneWithCachingStub(
         """
         model Person {
            id : PersonId inherits Int
            accountId : AccountId inherits Int
            name : PersonName inherits String
         }
         model AccountState {
            balance : AccountBalance inherits Decimal
         }
         service PersonApi {
            operation getPeople():Person[]
            @com.orbitalhq.caching.Cache(mode = CachePolicy.Disabled)
            operation getAccount(AccountId):AccountState
         }
      """.trimIndent()
      )
      stub.addResponse(
         "getPeople", """[
         |{"id" : 1, "accountId" : 100, "name" : "Jim" },
         |{"id" : 2, "accountId" : 100, "name" : "Jill" }
         |]""".trimMargin()
      )
      stub.addResponse("getAccount", """{ "balance" : 555.00 }""")
      val result = vyne.query(
         """find { Person[] } as {
         |  name : PersonName
         |  balance: AccountBalance
         |}[]
      """.trimMargin()
      ).rawObjects()
      result.shouldBe(
         listOf(
            mapOf("name" to "Jim", "balance" to 555.00.toBigDecimal()),
            mapOf("name" to "Jill", "balance" to 555.00.toBigDecimal())
         )
      )

      // balance service should've been called twice,
      // as caching is disabled on this operation
      stub.calls["getAccount"].shouldHaveSize(2)
   }

   @Test
   fun `a model tagged as do not cache is never cached`() : Unit = runBlocking{
      val (vyne, stub) = vyneWithCachingStub(
         """
         model Person {
            id : PersonId inherits Int
            accountId : AccountId inherits Int
            name : PersonName inherits String
         }
         @com.orbitalhq.caching.Cache(mode = CachePolicy.Disabled)
         model AccountState {
            balance : AccountBalance inherits Decimal
         }
         service PersonApi {
            operation getPeople():Person[]
            operation getAccount(AccountId):AccountState
         }
      """.trimIndent()
      )
      stub.addResponse(
         "getPeople", """[
         |{"id" : 1, "accountId" : 100, "name" : "Jim" },
         |{"id" : 2, "accountId" : 100, "name" : "Jill" }
         |]""".trimMargin()
      )
      stub.addResponse("getAccount", """{ "balance" : 555.00 }""")
      val result = vyne.query(
         """find { Person[] } as {
         |  name : PersonName
         |  balance: AccountBalance
         |}[]
      """.trimMargin()
      ).rawObjects()
      result.shouldBe(
         listOf(
            mapOf("name" to "Jim", "balance" to 555.00.toBigDecimal()),
            mapOf("name" to "Jill", "balance" to 555.00.toBigDecimal())
         )
      )

      // balance service should've been called twice,
      // as caching is disabled on the return type
      stub.calls["getAccount"].shouldHaveSize(2)
   }

   /**
    * This test explores long-running streaming queries, where a service is hit and items are
    * cached. However in a stream that runs for several days, we may want to expire that result
    * from the per-query cache, so that it is hit again.
    */
   @Test
   fun `an item stored in a query cache is evicted after ttl expires`() {

   }


   private fun vyneWithCachingStub(schemaSrc: String): Pair<Vyne, StubService> {
      var stub: StubService? = null
      val vyne = testVyne(
         listOf(
            CacheAnnotation.CacheTaxi,
            schemaSrc
         ),
      ) { schema ->
         stub = StubService(schema = schema)

         val cacheDecorator = CacheAwareOperationInvocationDecorator(
            stub!!,
            OperationCacheFactory.default().getOperationCache(QueryScopedCache)
         )
         listOf(cacheDecorator)
      }
      return vyne to stub!!
   }
}
