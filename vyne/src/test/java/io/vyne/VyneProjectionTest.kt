package io.vyne

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.*
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.parseJsonModel
import io.vyne.models.json.parseKeyValuePair
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import org.junit.Test
import java.math.BigDecimal

class VyneProjectionTest {
   val testSchema = """
// Primitives
type alias OrderId as String
type alias TradeId as String
type alias OrderDate as Date
type alias Price as Decimal
type alias TradeNo as String

model CommonOrder {
   id: OrderId
   date: OrderDate
   tradeNo : TradeNo
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
   broker1TradeId: TradeId
}

type Broker1Trade inherits Trade {
   broker1TradeID: TradeId
   broker1OrderID: OrderId
   broker1Price: Price
   broker1TradeNo: TradeNo
}

// services
service Broker1Service {
   operation getBroker1Orders( start : OrderDate, end : OrderDate) : Broker1Order[] (OrderDate >= start, OrderDate < end)
   operation getBroker1Trades( orderId: OrderId) : Broker1Trade[]
   operation findOneByOrderId( orderId: OrderId ) : Broker1Trade
   operation getBroker1TradesForOrderIds( orderIds: OrderId[]) : Broker1Trade[]
   operation findSingleByOrderID(  id : OrderId ) : Broker1Order( OrderId = id )

}

""".trimIndent()

   @Test
   fun `project by enriching from other services`() {
      val schema = """
         type Id inherits String
         type Field1 inherits String
         type ParentType inherits String
         type ChildType inherits ParentType
         type GrandChildType inherits ChildType
         type ProvidedByService inherits String

         model ServiceData {
            field: ProvidedByService
         }

         model Target {
               id: Id
               field1: Field1
               field2: ProvidedByService
         }
         model Order {
            id: Id
            field1: Field1
            child: ChildType
         }

         @DataSource
         service HelperService {
            operation getData( input : ChildType) : ServiceData
         }

         service OrderService {
            operation findAll( ) : Order[]
         }

      """.trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getData", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            return vyne.addJsonModel("ServiceData", """
               {
                   "field": "This is Provided By External Service"
               }
            """.trimIndent())
         }
      })

      stubService.addResponse("findAll", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            return vyne.addJsonModel("Order[]", """
               [
               {
                   "id": "id1",
                   "field1": "Field - 1",
                   "child": "Child 1"
               },
               {
                   "id": "id2",
                   "field1": "Field - 2",
                   "child": "Child 2"
               }
               ]
            """.trimIndent())
         }
      })

      val result = vyne.query("""
         findAll {
            Order[]
         } as Target[]""".trimIndent())
      result.isFullyResolved.should.be.`true`
      val results = result.resultMap[result.resultMap.keys.first()] as List<Map<String, Any>>
      results.first().should.contain(Pair("field2", "This is Provided By External Service"))
      results[1].should.contain(Pair("field2", "This is Provided By External Service"))
   }

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
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.addJsonModel("Broker1Order[]", generateBroker1OrdersWithTraderId(noOfRecords))
         }
      })
      stubService.addResponse("getBroker2Orders", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.addJsonModel("Broker2Order[]", "[]")
         }
      })
      stubService.addResponse("getUserNameFromId", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
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
         buf.append("""{ "broker1ID" : "broker1Order1", "broker1Date" : "2020-01-01", "traderId" : "trader${i % 2}"}""")
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
   instrument_type: InstrumentType
   description: InstrumentDescription
}

enum InstrumentType {
    Type1,
    Type2
}

enum OrderInstrumentType {
    OrderInstrumentType1 synonym of InstrumentType.Type1,
    OrderInstrumentType2 synonym of InstrumentType.Type2
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
   orderInstrumentType: OrderInstrumentType
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
      val noOfRecords = 10000

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getBroker1Orders", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.addJsonModel("Broker1Order[]", generateBroker1Orders(noOfRecords))
         }
      })
      stubService.addResponse("getInstrument", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val instrumentId = parameters[0].second.value as String
            val (instrumentDescription, instrumentType) = when (instrumentId) {
               "instrument0" -> "UST 2Y5Y10Y" to "Type1"
               "instrument1" -> "GBP/USD 1Year Swap" to "Type2"
               else -> TODO("Unknown userId=$instrumentId")
            }

            val instrumentResponse = """{"id":"$instrumentId", "description": "$instrumentDescription", "instrument_type": "$instrumentType"}"""
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
                  Pair("description", "UST 2Y5Y10Y"),
                  Pair("instrument_type", "Type1")
               )
            ),
            Pair("orderInstrumentType", "OrderInstrumentType1")
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
                  Pair("description", "GBP/USD 1Year Swap"),
                  Pair("instrument_type", "Type2")
               )
            ),
            Pair("orderInstrumentType", "OrderInstrumentType2")
         )
      )
   }

   @Test
   fun `project to CommonOrder with Trades`() {
      // TODO confirm how the mappings should look like
      val noOfRecords = 1000
      val schema = """
// Primitives
type alias OrderId as String
type alias TradeId as String
type alias OrderDate as Date
type alias Price as Decimal
type alias TradeNo as String
type alias IdentifierClass as String

enum Direction {
   BUY,
   SELL
}

model CommonOrder {
   id: OrderId
   date: OrderDate
   tradeNo : TradeNo
   identifierType: IdentifierClass
   direction: Direction
}

model CommonTrade {
   id: TradeId
   orderId: OrderId
   price: Price
}

model Order {}
model Trade {}

type extension CommonOrder {
   identifierType: IdentifierClass by default('ISIN')
   direction: Direction by default (Direction.SELL)
}

// Broker specific types
type Broker1Order inherits Order {
   broker1ID: OrderId
   broker1Date: OrderDate
   broker1TradeId: TradeId
}

type Broker1Trade inherits Trade {
   broker1TradeID: TradeId
   broker1OrderID: OrderId
   broker1Price: Price
   broker1TradeNo: TradeNo
}

// services
service Broker1Service {
   operation getBroker1Orders( start : OrderDate, end : OrderDate) : Broker1Order[] (OrderDate >= start, OrderDate < end)
   operation getBroker1Trades( orderId: OrderId) : Broker1Trade[]
   operation findOneByOrderId( orderId: OrderId ) : Broker1Trade
   operation getBroker1TradesForOrderIds( orderIds: OrderId[]) : Broker1Trade[]
}

""".trimIndent()

      val (vyne, stubService) = testVyne(schema)
      val orders = generateBroker1Orders(noOfRecords)
      val trades = generateOneBroker1TradeForEachOrder(noOfRecords)
      stubService.addResponse("getBroker1Orders", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.parseJsonModel("Broker1Order[]", orders)
         }
      })

      stubService.addResponse("getBroker1Trades", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            return vyne.parseJsonModel("Broker1Trade[]", trades)
         }
      })

      var getBroker1TradesForOrderIdsInvocationCount = 0
      stubService.addResponse("getBroker1TradesForOrderIds", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderIds = parameters[0].second.value as List<TypedValue>
            val buf = StringBuilder("[")
            orderIds.forEachIndexed { index, typedValue ->
               generateBroker1Trades(typedValue.value as String, index, buf)
               if (index < orderIds.size - 1) {
                  buf.append(",")
               }
            }
            buf.append("]")
            getBroker1TradesForOrderIdsInvocationCount++
            return vyne.parseJsonModel("Broker1Trade[]", buf.toString().trimIndent())
         }
      })

      var findOneByOrderIdInvocationCount = 0
      stubService.addResponse("findOneByOrderId", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderId = parameters[0].second.value as String
            findOneByOrderIdInvocationCount++
            return vyne.parseJsonModel("Broker1Trade", """
               {
                  "broker1OrderID" : "broker1Order$orderId",
                  "broker1TradeID" : "trade_id_$orderId",
                  "broker1Price"   : 10.1,
                  "broker1TradeNo": "trade_no_$orderId"
               }
            """.trimIndent())
         }
      })

      // act
      val result = vyne.query("""findAll { Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30") } as CommonOrder[]""".trimIndent())

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      resultList.size.should.be.equal(noOfRecords)
      resultList.forEachIndexed { index, result ->
         result.should.equal(
            mapOf(
               Pair("id", "broker1Order$index"),
               Pair("date", "2020-01-01"),
               Pair("tradeNo", "trade_no_$index"),
               Pair("identifierType", "ISIN"),
               Pair("direction", "Direction.SELL")
            )
         )
      }

      findOneByOrderIdInvocationCount.should.equal(0)
      getBroker1TradesForOrderIdsInvocationCount.should.equal(1)
   }

   @Test
   fun `One to Many Mapping Projection with a date between query`() {
      val (vyne, stubService) = testVyne(testSchema)
      // 1 order and 3 matching trades.
      val numberOfOrders = 1
      val numberOfCorrespondingTrades = 3
      // Generate 2 orders/
      // 1 order will have 3 corresponding trades whereas the other order has none..
      val orders = generateBroker1Orders(numberOfOrders + 1)
      stubService.addResponse("getBroker1Orders", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.parseJsonModel("Broker1Order[]", orders)
         }
      })

      var getBroker1TradesForOrderIdsInvocationCount = 0
      stubService.addResponse("getBroker1TradesForOrderIds", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderIds = parameters[0].second.value as List<TypedValue>
            val buf = StringBuilder("[")
            (0 until numberOfCorrespondingTrades).forEach { index ->
               generateBroker1Trades(orderIds.first().value as String, 0, buf, index, "10.$index")
               if (index < 2) {
                  buf.append(",")
               }
            }
            buf.append("]")
            getBroker1TradesForOrderIdsInvocationCount++
            return vyne.parseJsonModel("Broker1Trade[]", buf.toString().trimIndent())
         }
      })

      var findOneByOrderIdInvocationCount = 0
      stubService.addResponse("findOneByOrderId", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderId = parameters[0].second.value as String
            findOneByOrderIdInvocationCount++

            return TypedNull.create(vyne.type("Broker1Trade"))
         }
      })

      // act
      val result = vyne.query("""findAll { Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30") } as CommonOrder[]""".trimIndent())

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      resultList.size.should.be.equal(numberOfCorrespondingTrades + 1)
      resultList.forEachIndexed { index, result ->
         if (index < 3) {
            // First three results corresponding to 1 order 3 trades setup
            result.should.equal(
               mapOf(
                  Pair("id", "broker1Order0"),
                  Pair("date", "2020-01-01"),
                  Pair("tradeNo", "trade_no_$index")
               )
            )
         } else {
            //last result, corresponding to an order without a trade and hence it doesn't contain a tradeNo.
            result.should.equal(
               mapOf(
                  "id" to "broker1Order1",
                  "date" to "2020-01-01",
                  "tradeNo" to null // See TypedObjectFactory.build() for discussion on returning nulls
               )
            )
         }
      }
      findOneByOrderIdInvocationCount.should.equal(1) // 1 call for the order without a trade.
      getBroker1TradesForOrderIdsInvocationCount.should.equal(1)
   }

   @Test
   fun `One to Many Mapping Projection with an Id equals query`() {
      val (vyne, stubService) = testVyne(testSchema)
      // 1 order and 3 matching trades.
      val numberOfOrders = 1
      val numberOfCorrespondingTrades = 3
      // Generate 2 orders/
      // 1 order will have 3 corresponding trades whereas the other order has none..
      val orders = generateBroker1Orders(numberOfOrders + 1)
      stubService.addResponse("getBroker1Orders", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.parseJsonModel("Broker1Order[]", orders)
         }
      })

      var getBroker1TradesForOrderIdsInvocationCount = 0
      stubService.addResponse("getBroker1TradesForOrderIds", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderIds = parameters[0].second.value as List<TypedValue>
            val buf = StringBuilder("[")
            (0 until numberOfCorrespondingTrades).forEach { index ->
               generateBroker1Trades(orderIds.first().value as String, 0, buf, index, "10.$index")
               if (index < 2) {
                  buf.append(",")
               }
            }
            buf.append("]")
            getBroker1TradesForOrderIdsInvocationCount++
            return vyne.parseJsonModel("Broker1Trade[]", buf.toString().trimIndent())
         }
      })

      var findOneByOrderIdInvocationCount = 0
      stubService.addResponse("findOneByOrderId", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderId = parameters[0].second.value as String
            findOneByOrderIdInvocationCount++

            return TypedNull.create(vyne.type("Broker1Trade"))
         }
      })

      //find by order Id and project
      stubService.addResponse("findSingleByOrderID", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            return if (parameters.first().second.value == "broker1Order0") {
               vyne.parseJsonModel("Broker1Order", generateBroker1Order(0))
            } else {
               vyne.parseJsonModel("Broker1Order", "{}")
            }
         }
      })

      val findByOrderIdResult = vyne.query("""findAll { Order (OrderId = "broker1Order0") } as CommonOrder[]""".trimIndent())
      expect(findByOrderIdResult.isFullyResolved).to.be.`true`
      val findByOrderIdResultList = findByOrderIdResult.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      findByOrderIdResultList.size.should.be.equal(numberOfCorrespondingTrades)
   }

   @Test
   fun `One to Many Mapping Projection with an Id equals query returning zero match`() {
      val (vyne, stubService) = testVyne(testSchema)
      // 1 order and 3 matching trades.
      val numberOfOrders = 1
      val numberOfCorrespondingTrades = 3
      // Generate 2 orders/
      // 1 order will have 3 corresponding trades whereas the other order has none..
      val orders = generateBroker1Orders(numberOfOrders + 1)
      stubService.addResponse("getBroker1Orders", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.parseJsonModel("Broker1Order[]", orders)
         }
      })

      var getBroker1TradesForOrderIdsInvocationCount = 0
      stubService.addResponse("getBroker1TradesForOrderIds", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderIds = parameters[0].second.value as List<TypedValue>
            val buf = StringBuilder("[")
            (0 until numberOfCorrespondingTrades).forEach { index ->
               generateBroker1Trades(orderIds.first().value as String, 0, buf, index, "10.$index")
               if (index < 2) {
                  buf.append(",")
               }
            }
            buf.append("]")
            getBroker1TradesForOrderIdsInvocationCount++
            return vyne.parseJsonModel("Broker1Trade[]", buf.toString().trimIndent())
         }
      })

      var findOneByOrderIdInvocationCount = 0
      stubService.addResponse("findOneByOrderId", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderId = parameters[0].second.value as String
            findOneByOrderIdInvocationCount++

            return TypedNull.create(vyne.type("Broker1Trade"))
         }
      })

      //find by order Id and project
      stubService.addResponse("findSingleByOrderID", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            return if (parameters.first().second.value == "broker1Order0") {
               vyne.parseJsonModel("Broker1Order", generateBroker1Order(0))
            } else {
               TypedNull.create(vyne.type("Broker1Order"))
//               vyne.parseJsonModel("Broker1Order", "{}")
            }
         }
      })
      // find by a non-existing order Id and project
      val noResult = vyne.query("""findAll { Order (OrderId = "MY SPECIAL ORDER ID") } as CommonOrder[]""".trimIndent())
      val noResultList = noResult.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      noResultList.should.be.empty
   }

   @Test
   fun `Multiple orders with same id and multiple trades with same order Id`() {
      val (vyne, stubService) = testVyne(testSchema)
      val numberOfCorrespondingTrades = 3
      // 2 orders (with same id) will have 3 corresponding trades
      val orders = StringBuilder("[")
         .append(generateBroker1Order(0, "2020-01-01"))
         .append(",")
         .append(generateBroker1Order(0, "2021-01-01"))
         .append("]")
         .toString()
      stubService.addResponse("getBroker1Orders", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.parseJsonModel("Broker1Order[]", orders)
         }
      })

      var getBroker1TradesForOrderIdsInvocationCount = 0
      stubService.addResponse("getBroker1TradesForOrderIds", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderIds = parameters[0].second.value as List<TypedValue>
            val buf = StringBuilder("[")
            (0 until numberOfCorrespondingTrades).forEach { index ->
               generateBroker1Trades(orderIds.first().value as String, 0, buf, index, "10.$index")
               if (index < 2) {
                  buf.append(",")
               }
            }
            buf.append("]")
            getBroker1TradesForOrderIdsInvocationCount++
            return vyne.parseJsonModel("Broker1Trade[]", buf.toString().trimIndent())
         }
      })

      var findOneByOrderIdInvocationCount = 0
      stubService.addResponse("findOneByOrderId", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            val orderId = parameters[0].second.value as String
            findOneByOrderIdInvocationCount++

            return TypedNull.create(vyne.type("Broker1Trade"))
         }
      })

      // act
      val result = vyne.query("""findAll { Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30") } as CommonOrder[]""".trimIndent())

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      resultList.size.should.be.equal(2 * numberOfCorrespondingTrades)
      resultList.forEachIndexed { index, result ->
         if (index < numberOfCorrespondingTrades) {
            // First three results corresponding to 1 order 3 trades setup
            result.should.equal(
               mapOf(
                  Pair("id", "broker1Order0"),
                  Pair("date", "2020-01-01"),
                  Pair("tradeNo", "trade_no_$index")
               )
            )
         } else {
            result.should.equal(
               mapOf(
                  Pair("id", "broker1Order0"),
                  Pair("date", "2021-01-01"),
                  Pair("tradeNo", "trade_no_${index - numberOfCorrespondingTrades}")
               )
            )
         }
      }
      findOneByOrderIdInvocationCount.should.equal(0)
      getBroker1TradesForOrderIdsInvocationCount.should.equal(1)
   }

   private fun generateBroker1Trades(orderId: String, index: Int, buf: StringBuilder, tradeId: Int? = null, price: String? = null): StringBuilder {
      val brokerTraderId = tradeId?.let { "trade_id_$it" } ?: "trade_id_$index"
      val brokerTradeNo = tradeId?.let { "trade_no_$it" } ?: "trade_no_$index"
      val brokerPrice = price ?: "10.1"
      buf.append("""
         {
            "broker1OrderID" : "$orderId",
            "broker1TradeID" :  "$brokerTraderId",
            "broker1Price"   : "$brokerPrice",
            "broker1TradeNo": "$brokerTradeNo"
         }
         """.trimMargin())
      return buf
   }

   private fun generateOneBroker1TradeForEachOrder(noOfRecords: Int): String {
      val buf = StringBuilder("[")
      for (i in 0 until noOfRecords) {
         buf.append("""
         {
            "broker1OrderID" : "broker1Order$i",
            "broker1TradeID" : "trade_id_$i",
            "broker1Price"   : "10.1",
            "broker1TradeNo": "trade_no_$i"
         }
         """.trimMargin())
         if (i < noOfRecords - 1) {
            buf.append(",")
         }
      }
      buf.append("]")
      return buf.toString()
   }

   private fun generateBroker1Order(intSuffix: Int, date: String = "2020-01-01"): String {
      return """
         {
            "broker1ID" : "broker1Order${intSuffix}",
            "broker1Date" : "$date",
            "broker1Direction" :
            "bankbuys",
            "instrumentId" : "instrument${intSuffix % 2}",
            "broker1TradeId": "trade_id_$intSuffix"
         }
         """.trimMargin()
   }

   private fun generateBroker1Orders(noOfRecords: Int): String {
      val buf = StringBuilder("[")
      for (i in 0 until noOfRecords) {
         buf.append(generateBroker1Order(i))
         if (i < noOfRecords - 1) {
            buf.append(",")
         }
      }
      buf.append("]")
      return buf.toString()
   }

   @Test
   fun `missing Country does not break Client projection`() {
      // prepare
      val testSchema = """
         model Client {
            name : PersonName as String
            country : CountryCode as String
         }
         model Country {
             countryCode : CountryCode
             countryName : CountryName as String
         }
         model ClientAndCountry {
            personName : PersonName
            countryName : CountryName
         }

         service MultipleInvocationService {
            operation getCustomers():Client[]
            operation getCountry(CountryCode): Country
         }
      """.trimIndent()

      val (vyne, stubService) = testVyne(testSchema)
      stubService.addResponse("getCustomers", vyne.parseJsonModel("Client[]", """
         [
            { name : "Jimmy", country : "UK" },
            { name : "Devrim", country : "TR" }
         ]
         """.trimIndent()))

      stubService.addResponse("getCountry", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            val countryCode = parameters.first().second.value!!.toString()
            return if (countryCode == "UK") {
               vyne.parseJsonModel("Country", """{"countryCode": "UK", "countryName": "United Kingdom"}""")
            } else {
               TypedObject(vyne.schema.type("Country"), emptyMap(), Provided)
            }
         }
      })

      // act
      val result = vyne.query("""findAll { Client[] } as ClientAndCountry[]""".trimIndent())

      // assert
      result.resultMap.get("lang.taxi.Array<ClientAndCountry>").should.be.equal(
         listOf(
            mapOf("personName" to "Jimmy", "countryName" to "United Kingdom"),
            mapOf("personName" to "Devrim", "countryName" to null)
         )
      )
   }

   @Test
   fun `duplicate matches with same values in projection is resolved without any errors`() {
      // prepare
      val testSchema = """
         type OrderId inherits String
         type TraderName inherits String
         type InstrumentId inherits String
         type MaturityDate inherits Date
         type TradeId inherits String
         type InstrumentName inherits String
         model Order {
            orderId: OrderId
            traderName : TraderName
            instrumentId: InstrumentId
         }
         model Instrument {
             instrumentId: InstrumentId
             maturityDate: MaturityDate
             name: InstrumentName
         }
         model Trade {
            orderId: OrderId
            maturityDate: MaturityDate
            tradeId: TradeId
         }

         model Report {
            orderId: OrderId
            tradeId: TradeId
            instrumentName: InstrumentName
            maturityDate: MaturityDate
            traderName : TraderName
         }

         service MultipleInvocationService {
            operation getOrders(): Order[]
            operation getTrades(orderIds: OrderId): Trade
            operation getTrades(orderIds: OrderId[]): Trade[]
            operation getInstrument(instrumentId: InstrumentId): Instrument
         }
      """.trimIndent()

      val maturityDate = "2025-12-01"
      val (vyne, stubService) = testVyne(testSchema)
      stubService.addResponse("getOrders", vyne.parseJsonModel("Order[]", """
         [
            {
               "orderId": "orderId_0",
               "traderName": "john",
               "instrumentId": "Instrument_0"
            }
         ]
         """.trimIndent()))

      stubService.addResponse("getInstrument", vyne.parseJsonModel("Instrument", """
            {
               "maturityDate": "$maturityDate",
               "instrumentId": "Instrument_0",
               "name": "2040-11-20 0.1 Bond"
            }
         """.trimIndent()))

      stubService.addResponse("getTrades", vyne.parseJsonModel("Trade[]", """
            [{
               "maturityDate": "$maturityDate",
               "orderId": "orderId_0",
               "tradeId": "Trade_0"
            }]
         """.trimIndent()))
      val result = vyne.query("""findAll { Order[] } as Report[]""".trimIndent())
      result.isFullyResolved.should.be.`true`
      result.resultMap.get("lang.taxi.Array<Report>").should.be.equal(
         listOf(
            mapOf(
               "orderId" to "orderId_0",
               "traderName" to "john",
               "tradeId" to "Trade_0",
               "instrumentName" to "2040-11-20 0.1 Bond",
               "maturityDate" to "$maturityDate")
         )
      )
   }


   @Test
   fun `can use calculated fields on output models`() {
      val (vyne,_) = testVyne("""
         type Quantity inherits Int
         type Value inherits Int
         type Cost inherits Int
         model Input {
            qty : Quantity
            value : Value
         }
         model Output {
            qty : Quantity
            value : Value
            cost : Cost by (qty * value)
         }
      """.trimIndent())
      val input = vyne.parseJsonModel("Input", """{ "qty" : 100, "value" : 2 }""", source = Provided)
      val result = vyne.from(input).build("Output")
      val output = result["Output"] as TypedObject
      output["cost"].value.should.equal(200)
   }
   @Test
   fun `can use when by`() {
      val (vyne,_) = testVyne("""
         model Input {
            str: String
            value : Decimal?
         }

         enum PriceType {
            Percentage("%"),
            Basis("Bps")
         }

         model SampleType {
            price: Decimal?
            tempPriceType: String?
            priceType: PriceType? by when {
                this.price = null -> null
                this.price != null -> tempPriceType
            }
         }
      """.trimIndent())
      val input = vyne.parseJsonModel("Input", """{ "value": 100, "str": "Percentage" }""", source = Provided)
      val result = vyne.from(input).build("SampleType")
      val output = result["SampleType"] as TypedObject
      output["priceType"].value.should.equal("Percentage")
   }


   @Test
   fun `A service annotated with @DataSource will not be invoked twice`() {
      val testSchema = """
         model Client {
            name : PersonName as String
            country : CountryCode as String
         }
         model Country {
             countryCode : CountryCode
             countryName : CountryName as String
         }
         model ClientAndCountry {
            personName : PersonName
            countryName : CountryName
         }

         @Datasource
         service MultipleInvocationService {
            operation getCustomers():Client[]
            operation getCountry(CountryCode): Country
         }
      """.trimIndent()

      var getCountryInvoked = false
      val (vyne, stubService) = testVyne(testSchema)
      stubService.addResponse("getCustomers", vyne.parseJsonModel("Client[]", """
         [
            { name : "Jimmy", country : "UK" },
            { name : "Devrim", country : "TR" }
         ]
         """.trimIndent()))

      stubService.addResponse("getCountry", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            getCountryInvoked = true
            val countryCode = parameters.first().second.value!!.toString()
            return if (countryCode == "UK") {
               vyne.parseJsonModel("Country", """{"countryCode": "UK", "countryName": "United Kingdom"}""")
            } else {
               TypedObject(vyne.schema.type("Country"), emptyMap(), Provided)
            }
         }
      })

      // act
      val result = vyne.query("""findAll { Client[] } as ClientAndCountry[]""".trimIndent())

      // assert
      result.resultMap["lang.taxi.Array<ClientAndCountry>"].should.be.equal(
         listOf(
            mapOf("personName" to "Jimmy", "countryName" to null),
            mapOf("personName" to "Devrim", "countryName" to null) // See TypedObjectFactory.build() for discussion on returning nulls
         )
      )
      getCountryInvoked.should.be.`false`
   }


   @Test
   fun `should output offset  correctly`() {
      val (vyne, stubService) = testVyne("""
         model InputModel {
           inputField: Instant( @format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
         }

         model OutputModel {
            myField : Instant( @format = ["yyyy-MM-dd'T'HH:mm:ss.SSSZ"], @offset = 60 )
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }
      """.trimIndent())

      val inputInstant1 = "2020-08-19T13:07:09.591Z"
      val inputInstant2 = "2020-08-18T13:07:09.591Z"
      val outputInstant1 = "2020-08-19T14:07:09.591+0100"
      val outputInstant2 = "2020-08-18T14:07:09.591+0100"
      stubService.addResponse("getInputData", vyne.parseJsonModel("InputModel[]", """
         [
            { "inputField": "$inputInstant1" },
            { "inputField": "$inputInstant2" }
         ]
         """.trimIndent()))
      val result =  vyne.query("""findAll { InputModel[] } as OutputModel[]""".trimIndent())
      result.resultMap["lang.taxi.Array<OutputModel>"].should.be.equal(
         listOf(
            mapOf("myField" to "$outputInstant1" ),
            mapOf("myField" to "$outputInstant2")
         )
      )
   }

   @Test
   fun `should output offset correctly without any format`() {
      val (vyne, stubService) = testVyne("""
         model InputModel {
           inputField: Instant( @format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
         }

         model OutputModel {
            myField : Instant( @offset = 60 )
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }
      """.trimIndent())

      val inputInstant1 = "2020-08-19T13:07:09.591Z"
      val inputInstant2 = "2020-08-18T13:07:09.591Z"
      val outputInstant1 = "2020-08-19T14:07:09.591+01"
      val outputInstant2 = "2020-08-18T14:07:09.591+01"
      stubService.addResponse("getInputData", vyne.parseJsonModel("InputModel[]", """
         [
            { "inputField": "$inputInstant1" },
            { "inputField": "$inputInstant2" }
         ]
         """.trimIndent()))
      val result =  vyne.query("""findAll { InputModel[] } as OutputModel[]""".trimIndent())
      result.resultMap["lang.taxi.Array<OutputModel>"].should.be.equal(
         listOf(
            mapOf("myField" to "$outputInstant1" ),
            mapOf("myField" to "$outputInstant2")
         )
      )
   }

   @Test
   fun `Calculated fields should be correctly set in projected type`() {
      val (vyne, stubService) = testVyne("""
         type QtyFill inherits Decimal
         type UnitMultiplier inherits Decimal
         type FilledNotional inherits Decimal

         model InputModel {
           multiplier: UnitMultiplier by default(2)
           qtyFill: QtyFill
         }

         model OutputModel {
            qtyHit : QtyFill?
            unitMultiplier: UnitMultiplier?
            filledNotional : FilledNotional?  by (this.qtyHit * this.unitMultiplier)
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }
      """.trimIndent())

      stubService.addResponse("getInputData", vyne.parseJsonModel("InputModel[]", """
         [
            { "qtyFill": 200, "multiplier": 2 }
         ]
         """.trimIndent()))
      val result =  vyne.query("""findAll { InputModel[] } as OutputModel[]""".trimIndent())
      result.resultMap["lang.taxi.Array<OutputModel>"].should.be.equal(
         listOf(
            mapOf(
               "qtyHit" to BigDecimal("200"),
               "unitMultiplier" to BigDecimal("2"),
               "filledNotional" to BigDecimal("400")
            )
         )
      )
   }

   @Test
   fun `should project to anonymous type extending discovery type`() {
      val (vyne, stubService) = testVyne("""
         type QtyFill inherits Decimal
         type UnitMultiplier inherits Decimal
         type FilledNotional inherits Decimal
         type InputId inherits String

         model InputModel {
           multiplier: UnitMultiplier by default(2)
           qtyFill: QtyFill
           id: InputId
         }

         model OutputModel {
            qtyHit : QtyFill?
            unitMultiplier: UnitMultiplier?
            filledNotional : FilledNotional?  by (this.qtyHit * this.unitMultiplier)
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }
      """.trimIndent())

      stubService.addResponse("getInputData", vyne.parseJsonModel("InputModel[]", """
         [
            { "qtyFill": 200, "multiplier": 2, "id": "input1" },
            { "qtyFill": 200, "multiplier": 2, "id": "input2" },
            { "qtyFill": 200, "multiplier": 2, "id": "input3" }
         ]
         """.trimIndent()))
      val result =  vyne.query("""
            findAll {
                InputModel[]
              } as {
                 id
               }[]
            """.trimIndent())

      result.resultMap.values.first().should.be.equal(
         listOf(
            mapOf("id" to "input1"), mapOf("id" to "input2"), mapOf("id" to "input3")
         )
      )
   }

   @Test
   fun `should project to anonymous type extending discovery type II`() {
      val (vyne, stubService) = testVyne("""
         type QtyFill inherits Decimal
         type UnitMultiplier inherits Decimal
         type FilledNotional inherits Decimal
         type InputId inherits String

         model InputModel {
           multiplier: UnitMultiplier by default(2)
           qtyFill: QtyFill
           id: InputId
         }

         model OutputModel {
            qtyHit : QtyFill?
            unitMultiplier: UnitMultiplier?
            filledNotional : FilledNotional?  by (this.qtyHit * this.unitMultiplier)
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }
      """.trimIndent())

      stubService.addResponse("getInputData", vyne.parseJsonModel("InputModel[]", """
         [
            { "qtyFill": 200, "multiplier": 1, "id": "input1" },
            { "qtyFill": 200, "multiplier": 2, "id": "input2" },
            { "qtyFill": 200, "multiplier": 3, "id": "input3" }
         ]
         """.trimIndent()))
      val result =  vyne.query("""
            findAll {
                InputModel[]
              } as {
                 id
                 multiplier: UnitMultiplier
               }[]
            """.trimIndent())

      result.resultMap.values.first().should.be.equal(
         listOf(
            mapOf("id" to "input1", "multiplier" to BigDecimal("1")),
            mapOf("id" to "input2", "multiplier" to BigDecimal("2")),
            mapOf("id" to "input3", "multiplier" to BigDecimal("3"))
         )
      )
   }

   @Test
   fun `should project to anonymous type extending discovery type III`() {
      val (vyne, stubService) = testVyne("""
         type QtyFill inherits Decimal
         type UnitMultiplier inherits Decimal
         type FilledNotional inherits Decimal
         type InputId inherits String

         model InputModel {
           multiplier: UnitMultiplier by default(2)
           qtyFill: QtyFill
           id: InputId
         }

         model OutputModel {
            qtyHit : QtyFill?
            unitMultiplier: UnitMultiplier?
            filledNotional : FilledNotional?  by (this.qtyHit * this.unitMultiplier)
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }
      """.trimIndent())

      stubService.addResponse("getInputData", vyne.parseJsonModel("InputModel[]", """
         [
            { "qtyFill": 200, "multiplier": 1, "id": "input1" },
            { "qtyFill": 200, "multiplier": 2, "id": "input2" },
            { "qtyFill": 200, "multiplier": 3, "id": "input3" }
         ]
         """.trimIndent()))
      val result =  vyne.query("""
            findAll {
                InputModel[]
              } as OutputModel {
                 inputId: InputId
               }[]
            """.trimIndent())

      result.resultMap.values.first().should.be.equal(
         listOf(
            mapOf("qtyHit" to BigDecimal("200"), "unitMultiplier" to BigDecimal("1"), "filledNotional" to BigDecimal("200"), "inputId" to "input1"),
            mapOf("qtyHit" to BigDecimal("200"), "unitMultiplier" to BigDecimal("2"),  "filledNotional" to BigDecimal("400"), "inputId" to "input2"),
            mapOf("qtyHit" to BigDecimal("200"), "unitMultiplier" to BigDecimal("3"),  "filledNotional" to BigDecimal("600"), "inputId" to "input3")
         )
      )
   }


   @Test
   fun `avoid recursive parameter discovery`() {
      val (vyne, stubService) = testVyne("""
         type Isin inherits String
         type Ric inherits String
         type InstrumentIdentifierType inherits String
         type InputId inherits String


         model InputModel {
           id: InputId
           ric : Ric?
         }

         model OutputModel {
            isin: Isin

         }

         parameter model InstrumentReferenceRequest {
             Identifier : Ric?
             IdentifierType: InstrumentIdentifierType?
         }

         parameter model InstrumentReferenceResponse {
             ricCode : Ric?
             instrumentType: InstrumentIdentifierType?
             isin: Isin
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }

         service InstrumentService {
             operation getInstrumentFromRic( @RequestBody request:InstrumentReferenceRequest) :  InstrumentReferenceResponse
         }
      """.trimIndent())

      stubService.addResponse("getInputData", vyne.parseJsonModel("InputModel[]", """
         [
            {  "id": "input1", "ric": "ric1" },
            {  "id": "input2" },
            {  "id": "input3" }
         ]
         """.trimIndent()))
      val result =  vyne.query("""
            findAll {
                InputModel[]
              } as OutputModel []
            """.trimIndent())

      result.resultMap.values.first().should.be.equal(
         listOf(
            mapOf("isin" to null),
            mapOf("isin" to null),
            mapOf("isin" to null)
         )
      )

   }
}

