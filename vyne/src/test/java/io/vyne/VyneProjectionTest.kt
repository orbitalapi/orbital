package io.vyne

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.TypedInstance
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.parseKeyValuePair
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import org.junit.Ignore
import org.junit.Test


class VyneProjectionTest {

   @Test
   fun `project an array of Orders to the array of CommonOrder`() {
      // prepare
      val schema = """
type alias OrderDate as Date
type alias OrderId as String
type UserId inherits String
type UserName inherits String

model CommonOrder {
   id: OrderId
   date: OrderDate
   traderId: UserId
   traderName: UserName
}
model Order {
}
type Broker1Order inherits Order {
   broker1ID: OrderId
   broker1Date: OrderDate
   traderId: UserId
}
type Broker2Order inherits Order {
   broker2ID: OrderId
   broker2Date: OrderDate
}

// operations
service Broker1Service {
   operation getBroker1Orders( start : OrderDate, end : OrderDate) : Broker1Order[] (OrderDate >= start, OrderDate < end)
}
service Broker2Service {
   operation getBroker2Orders( start : OrderDate, end : OrderDate) : Broker2Order[] (OrderDate >= start, OrderDate < end)
}

service UserService {
   operation getUserNameFromId(userId: UserId):UserName
}

""".trimIndent()
      val noOfRecords = 10_000

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getBroker1Orders", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.addJsonModel("Broker1Order[]", generateBroker1OrdersWithTraderId(noOfRecords))
         }
      })
      stubService.addResponse("getBroker2Orders", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.addJsonModel("Broker2Order[]", "[]")
         }
      })
      stubService.addResponse("getUserNameFromId", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val userId = parameters[0].second.value as String
            val userName = when (userId) {
               "trader0" -> "John Smith"
               "trader1" -> "Mike Brown"
               else -> TODO("Unknown userId=$userId")
            }
            return vyne.parseKeyValuePair("UserName", userName)
         }
      })

      // act
      val result = vyne.query("""
         findAll {
            Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30")
         } as CommonOrder[]""".trimIndent())

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      resultList.size.should.be.equal(noOfRecords)
      resultList[0].should.equal(
         mapOf(
            Pair("id", "broker1Order1"),
            Pair("date", "2020-01-01"),
            Pair("traderId", "trader1"),
            Pair("traderName", "Mike Brown"))
      )
      resultList[1].should.equal(
         mapOf(
            Pair("id", "broker1Order1"),
            Pair("date", "2020-01-01"),
            Pair("traderId", "trader0"),
            Pair("traderName", "John Smith"))
      )
   }

   private fun generateBroker1OrdersWithTraderId(noOfRecords: Int): String {
      val buf = StringBuilder()
      buf.append("[")
      for (i in 1..noOfRecords) {
         buf.append("""{ "broker1ID" : "broker1Order1", "broker1Date" : "2020-01-01", "traderId" : "trader${i%2}"}""")
         if (i < noOfRecords) {
            buf.append(",")
         }
      }
      buf.append("]")
      return buf.toString()
   }

   @Test
   fun `project to CommonOrder and resolve Enum synonyms and Instruments`() {
      // prepare
      val schema = """
// Primitives
type alias OrderId as String
type alias OrderDate as Date
type InstrumentId inherits String
type InstrumentDescription inherits String

// common types
type Instrument {
   id: InstrumentId
   description: InstrumentDescription
}
enum BankDirection {
   BUY("sell"),
   SELL("buy")
}

model CommonOrder {
   id: OrderId
   date: OrderDate
   direction: BankDirection
   instrument: Instrument
}

model Order {}

// Broker specific types
enum Broker1Direction {
   BankBuys("bankbuys") synonym of BankDirection.BUY,
   BankSells("banksells") synonym of BankDirection.SELL
}
type Broker1Order inherits Order {
   broker1ID: OrderId
   broker1Date: OrderDate
   broker1Direction: Broker1Direction
   instrumentId: InstrumentId
}

// services
service Broker1Service {
   operation getBroker1Orders( start : OrderDate, end : OrderDate) : Broker1Order[] (OrderDate >= start, OrderDate < end)
}

service InstrumentService {
   operation getInstrument( instrument: InstrumentId ) : Instrument
}
         """.trimIndent()
      val noOfRecords = 100

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getBroker1Orders", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.addJsonModel("Broker1Order[]", generateBroker1Orders(noOfRecords))
         }
      })
      stubService.addResponse("getInstrument", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val instrumentId = parameters[0].second.value as String
            val instrumentDescription = when (instrumentId) {
               "instrument0" -> "UST 2Y5Y10Y"
               "instrument1" -> "GBP/USD 1Year Swap"
               else -> TODO("Unknown userId=$instrumentId")
            }
            val instrumentResponse = """{"id":"$instrumentId", "description": "$instrumentDescription"}"""
            return vyne.addJsonModel("Instrument", instrumentResponse)
         }
      })

      // act
      val result = vyne.query("""findAll { Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30") } as CommonOrder[]""".trimIndent())

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      resultList.size.should.be.equal(noOfRecords)
      resultList[0].should.equal(
         mapOf(
            Pair("id", "broker1Order0"),
            Pair("date", "2020-01-01"),
            Pair("direction", "sell"),
            Pair("instrument",
               mapOf(
                  Pair("id", "instrument0"),
                  Pair("description", "UST 2Y5Y10Y")
               )
            )
         )
      )
      resultList[1].should.equal(
         mapOf(
            Pair("id", "broker1Order1"),
            Pair("date", "2020-01-01"),
            Pair("direction", "sell"),
            Pair("instrument",
               mapOf(
                  Pair("id", "instrument1"),
                  Pair("description", "GBP/USD 1Year Swap")
               )
            )
         )
      )
   }

   private fun generateBroker1Orders(noOfRecords: Int): String {
      val buf = StringBuilder("[")
      for (i in 0 until noOfRecords) {
         buf.append("""
         {
            "broker1ID" : "broker1Order${i}",
            "broker1Date" : "2020-01-01",
            "broker1Direction" :
            "bankbuys",
            "instrumentId" : "instrument${i%2}"
         }
         """.trimMargin())
         if (i < noOfRecords -1) {
            buf.append(",")
         }
      }
      buf.append("]")
      return buf.toString()
   }

   @Test
   @Ignore("TODO Implement linking trades to order")
   fun `project to CommonOrder with Trades`() {
      // TODO confirm how the mappings should look like
      val noOfRecords = 100
      val schema = """
// Primitives
type alias OrderId as String
type alias TradeId as String
type alias OrderDate as Date
type alias Price as Double

model CommonOrder {
   id: OrderId
   date: OrderDate
   trades: CommonTrade[] // ??? Is this how we want to model this ???
}

model CommonTrade {
   id: TradeId
   orderId: OrderId
   price: Price
}

model Order {}
model Trade {}

// Broker specific types
type Broker1Order inherits Order {
   broker1ID: OrderId
   broker1Date: OrderDate
   broker1Trades: Broker1Trade[]
}

type Broker1Trade inherits Trade {
   broker1TradeID: TradeId
   broker1OrderID: OrderId
   broker1Price: Price
}

// services
service Broker1Service {
   operation getBroker1Orders( start : OrderDate, end : OrderDate) : Broker1Order[] (OrderDate >= start, OrderDate < end)
   operation getBroker1Trades( orderId: OrderId) : Broker1Trade[]
   //operation getBroker1Trades( orderId: OrderId[]) : Broker1Trade[]// this is more desired implementation
}

""".trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getBroker1Orders", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.addJsonModel("Broker1Order[]", generateBroker1Orders(noOfRecords))
         }
      })

      stubService.addResponse("getBroker1Trades", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderId = parameters[0].second.value as String
            return vyne.addJsonModel("Broker1Trade[]", generateBroker1Trades(orderId))
         }
      })

      // act
      val result = vyne.query("""findAll { Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30") } as CommonOrder[]""".trimIndent())

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      resultList.size.should.be.equal(noOfRecords)
      resultList[0].should.equal(
         mapOf(
            Pair("id", "broker1Order0"),
            Pair("date", "2020-01-01"),
            Pair("trades", "TODO")
         )
      )
   }

   private fun generateBroker1Trades(orderId: String): String {
      val noOfRecords = 2
      val buf = StringBuilder("[")
      for (i in 0 until noOfRecords) {
         buf.append("""
         {
            "broker1OrderID" : "$orderId",
            "broker1TradeID" : "${orderId}_$i",
            "broker1Price"   : "10.1"
         }
         """.trimMargin())
         if (i < noOfRecords - 1) {
            buf.append(",")
         }
      }
      buf.append("]")
      return buf.toString()
   }
}

