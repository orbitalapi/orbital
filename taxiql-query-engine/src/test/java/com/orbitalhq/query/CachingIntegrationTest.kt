package com.orbitalhq.query

import app.cash.turbine.test
import arrow.core.Either
import com.google.common.testing.FakeTicker
import com.orbitalhq.Vyne
import com.orbitalhq.expectTypedObject
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.tryParseJson
import com.orbitalhq.query.caching.CacheAnnotation
import com.orbitalhq.query.connectors.CacheAwareOperationInvocationDecorator
import com.orbitalhq.query.graph.operationInvocation.cache.OperationCacheFactory
import com.orbitalhq.rawObjects
import com.orbitalhq.schemas.QueryScopedCache
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyne
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

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
   fun `an item stored in a query cache is evicted after ttl declared on model expires`():Unit = runBlocking {
      val (vyne, stub, ticker) = vyneWithCachingStub(
         """
         model TransactionEvent {
            accountId : AccountId inherits Int
         }

         @com.orbitalhq.caching.Cache(maxIdleSeconds = 30)
         model AccountState {
            balance : AccountBalance inherits Decimal
         }
         service PersonApi {
            operation getAccount(AccountId):AccountState
            stream transactionEvents:Stream<TransactionEvent>
         }
      """.trimIndent()
      )
      stub.addResponse("getAccount", """{ "balance" : 555.00 }""")
      val transactionsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 1)
      stub.addResponseFlow("transactionEvents") { _, _ -> transactionsFlow }
      val results = vyne.query(
         """stream { TransactionEvent } as {
         |  id : AccountId
         |  balance: AccountBalance
         |}[]
      """.trimMargin()
      ).results

      results.test(10.seconds) {
         transactionsFlow.emit(vyne.tryParseJson("TransactionEvent", """{ "accountId": 1 }"""))
         val message1 = expectTypedObject().toRawObject()
         message1.shouldBe(mapOf("id" to 1, "balance" to 555.00.toBigDecimal()))
         // Should have made a single call
         stub.calls["getAccount"].shouldHaveSize(1)

         // Move time forwards 10 seconds, TTL hasn't expired yet
         ticker.advance(Duration.ofSeconds(10))

         transactionsFlow.emit(vyne.tryParseJson("TransactionEvent", """{ "accountId": 1 }"""))
         expectTypedObject()

         // Results are still cached, so shouldn't have made any new calls
         stub.calls["getAccount"].shouldHaveSize(1)

         // Move time forwards 40 seconds, (making 50 total) - TTL has now expired
         ticker.advance(Duration.ofSeconds(40))

         transactionsFlow.emit(vyne.tryParseJson("TransactionEvent", """{ "accountId": 1 }"""))
         expectTypedObject()

         // Results in cache have expired, so should have made a second call
         stub.calls["getAccount"].shouldHaveSize(2)
      }
   }

   /**
    * This test explores long-running streaming queries, where a service is hit and items are
    * cached. However in a stream that runs for several days, we may want to expire that result
    * from the per-query cache, so that it is hit again.
    */
   @Test
   fun `an item stored in a query cache is evicted after ttl declared on operation expires`():Unit = runBlocking {
      val (vyne, stub, ticker) = vyneWithCachingStub(
         """
         model TransactionEvent {
            accountId : AccountId inherits Int
         }

         model AccountState {
            balance : AccountBalance inherits Decimal
         }
         service PersonApi {
            @com.orbitalhq.caching.Cache(maxIdleSeconds = 30)
            operation getAccount(AccountId):AccountState
            stream transactionEvents:Stream<TransactionEvent>
         }
      """.trimIndent()
      )
      stub.addResponse("getAccount", """{ "balance" : 555.00 }""")
      val transactionsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 1)
      stub.addResponseFlow("transactionEvents") { _, _ -> transactionsFlow }
      val results = vyne.query(
         """stream { TransactionEvent } as {
         |  id : AccountId
         |  balance: AccountBalance
         |}[]
      """.trimMargin()
      ).results

      results.test(10.seconds) {
         transactionsFlow.emit(vyne.tryParseJson("TransactionEvent", """{ "accountId": 1 }"""))
         val message1 = expectTypedObject().toRawObject()
         message1.shouldBe(mapOf("id" to 1, "balance" to 555.00.toBigDecimal()))
         // Should have made a single call
         stub.calls["getAccount"].shouldHaveSize(1)

         // Move time forwards 10 seconds, TTL hasn't expired yet
         ticker.advance(Duration.ofSeconds(10))

         transactionsFlow.emit(vyne.tryParseJson("TransactionEvent", """{ "accountId": 1 }"""))
         expectTypedObject()

         // Results are still cached, so shouldn't have made any new calls
         stub.calls["getAccount"].shouldHaveSize(1)

         // Move time forwards 40 seconds, (making 50 total) - TTL has now expired
         ticker.advance(Duration.ofSeconds(40))

         transactionsFlow.emit(vyne.tryParseJson("TransactionEvent", """{ "accountId": 1 }"""))
         expectTypedObject()

         // Results in cache have expired, so should have made a second call
         stub.calls["getAccount"].shouldHaveSize(2)
      }
   }


   /**
    * This test explores long-running streaming queries, where a service is hit and items are
    * cached. However in a stream that runs for several days, we may want to expire that result
    * from the per-query cache, so that it is hit again.
    */
   @Test
   fun `an item stored in a query cache without cache annotation is evicted after default ttl expires`():Unit = runBlocking {
      val (vyne, stub, ticker) = vyneWithCachingStub(
         """
         model TransactionEvent {
            accountId : AccountId inherits Int
         }

         model AccountState {
            balance : AccountBalance inherits Decimal
         }
         service PersonApi {
            operation getAccount(AccountId):AccountState
            stream transactionEvents:Stream<TransactionEvent>
         }
      """.trimIndent()
      )
      stub.addResponse("getAccount", """{ "balance" : 555.00 }""")
      val transactionsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 1)
      stub.addResponseFlow("transactionEvents") { _, _ -> transactionsFlow }
      val results = vyne.query(
         """stream { TransactionEvent } as {
         |  id : AccountId
         |  balance: AccountBalance
         |}[]
      """.trimMargin()
      ).results

      results.test(10.seconds) {
         transactionsFlow.emit(vyne.tryParseJson("TransactionEvent", """{ "accountId": 1 }"""))
         val message1 = expectTypedObject().toRawObject()
         message1.shouldBe(mapOf("id" to 1, "balance" to 555.00.toBigDecimal()))
         // Should have made a single call
         stub.calls["getAccount"].shouldHaveSize(1)

         // Move time forwards 10 seconds, TTL hasn't expired yet
         ticker.advance(Duration.ofSeconds(10))

         transactionsFlow.emit(vyne.tryParseJson("TransactionEvent", """{ "accountId": 1 }"""))
         expectTypedObject()

         // Results are still cached, so shouldn't have made any new calls
         stub.calls["getAccount"].shouldHaveSize(1)

         // Move time forwards the max TTL, which will expire the item (as we're already 10 seconds in)
         ticker.advance(CacheAwareOperationInvocationDecorator.DEFAULT_CACHE_TTL)

         transactionsFlow.emit(vyne.tryParseJson("TransactionEvent", """{ "accountId": 1 }"""))
         expectTypedObject()

         // Results in cache have expired, so should have made a second call
         stub.calls["getAccount"].shouldHaveSize(2)
      }
   }


   private fun vyneWithCachingStub(schemaSrc: String): Triple<Vyne, StubService, FakeTicker> {
      var stub: StubService? = null
      val ticker = FakeTicker()
      val vyne = testVyne(
         listOf(
            CacheAnnotation.CacheTaxi,
            schemaSrc
         ),
      ) { schema ->
         stub = StubService(schema = schema)

         val cacheDecorator = CacheAwareOperationInvocationDecorator(
            stub!!,
            OperationCacheFactory.default(
               ticker
            ).getOperationCache(QueryScopedCache)
         )
         listOf(cacheDecorator)
      }
      return Triple(vyne , stub!!, ticker)
   }
}
