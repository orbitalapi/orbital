package com.orbitalhq

import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.runBlocking
import org.junit.Test


class QueryWithRangeTest {

   @Test
   fun givenQueryWithRangeParameters_then_callsService():Unit = runBlocking {
      val schema = """
      type Trade {
         id : TradeId inherits String
         timestamp : TradeDate inherits Instant
      }
      service TradeService {
         operation findTrades(startDate:TradeDate,endDate:TradeDate) : Trade[](
            TradeDate >= startDate &&
            TradeDate < endDate
         )
      }
      """.trimIndent()
      val (vyne, stub) = testVyne(schema)
      stub.addResponse("findTrades", TypedInstance.from(vyne.type("Trade[]"), emptyList<Any>(), vyne.schema, source = Provided))
      vyne.query("""find { Trade[]( TradeDate >= '2020-05-10T10:00:00Z' && TradeDate < '2020-05-10T11:00:00Z' ) }""")
         .rawObjects()
      stub.calls["findTrades"].shouldHaveSize(1)
   }
}

