package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.models.json.parseJsonCollection
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withTimeout

class TypedObjectFactoryDeadlockSpec : DescribeSpec({

   describe("A dead lock that occurred in TypedObjectFactory") {
      it("should run a query designed to produce a deadlock, without producing a deadlock") {
         // Constrain Default dispatcher to make deadlock reproducible
         val threadCount = Runtime.getRuntime().availableProcessors()

         val (vyne, stubService) = testVyne(
            """
        type TradeId inherits String
        type Isin inherits String
        type UnresolvableType inherits String  // intentionally never provided

        model Trade {
            id: TradeId
            isin: Isin
        }

        model EnrichedTrade {
            id: TradeId
            isin: Isin
            mystery: UnresolvableType?  // will trigger handleTypeNotFound
        }

        @Datasource
        service TradeService {
            operation getTrades(): Trade[]
        }
        """.trimIndent()
         )

         // Generate enough records to saturate the Default thread pool
         val trades = (1..(threadCount * 3)).map { i ->
            """{ "id": "trade$i", "isin": "US00000$i" }"""
         }.joinToString(",", "[", "]")

         stubService.addResponse(
            "getTrades",
            vyne.parseJson("Trade[]", trades)
         )

         // withTimeout will expose a deadlock that would otherwise hang the test suite forever
         withTimeout(10_000) {
            val result = vyne.query(
               """
            find { Trade[] } as EnrichedTrade[]
            """.trimIndent()
            )
            result.rawObjects() // force collection, triggering concurrent object construction
         }
      }


      it("should run another query designed to produce a deadlock, wihtout producing a deadlock") {
         val (vyne, stubService) = testVyne(
            """
        type Isin inherits String
        type Ric inherits String

        model InputModel {
            ric: Ric
        }

        model OutputModel {
            isin: Isin
        }

        parameter model RicRequest {
            ric: Ric
        }

        model RicResponse {
            isin: Isin
        }

        @Datasource
        service InputService {
            operation getInputs(): InputModel[]
        }

        service IsinLookupService {
            operation lookup(@RequestBody req: RicRequest): RicResponse
        }
    """.trimIndent()
         )

         // Generate enough rows to exhaust the DefaultDispatcher thread pool
         // The pool size is typically Runtime.availableProcessors(), so generate more than that
         val rowCount = Runtime.getRuntime().availableProcessors() * 3

         stubService.addResponse(
            "getInputs", vyne.parseJsonCollection(
               "InputModel[]",
               (1..rowCount).joinToString(",", "[", "]") { """{"ric": "ric$it"}""" }
            )
         )

         // IsinLookupService is NOT stubbed - it will trigger handleTypeNotFound
         // which calls runBlocking { findType() }

         val result = withTimeout(10_000) {
            val queryResult = vyne.query("find { InputModel[] } as OutputModel[]")
            queryResult.rawObjects()
         }

         // If we get here without timeout, the fix works
         result.size shouldBe rowCount
      }
   }
})
