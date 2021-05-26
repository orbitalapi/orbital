package io.vyne

import app.cash.turbine.test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.base.Stopwatch
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.*
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseJsonCollection
import io.vyne.models.json.parseJsonModel
import io.vyne.models.json.parseKeyValuePair
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Benchmark
import io.vyne.utils.StrategyPerformanceProfiler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.utils.log
import lang.taxi.utils.quoted
import org.junit.Ignore
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
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
   @Id
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
   fun `can perform simple projection`() = runBlocking {
      val (vyne, _) = testVyne(
         """
         type FirstName inherits String
         type LastName inherits String
         type PersonId inherits String
         model Person {
            id : PersonId
            firstName : FirstName
            lastName : LastName
         }
      """.trimIndent()
      )
      vyne.addModel(vyne.parseJsonModel("Person", """{ "id" : "1" , "firstName" : "Jimmy", "lastName" : "Schmit" } """))
      val result = vyne.query("""findOne { Person } as { first : FirstName }""")
      val list = result.rawResults
         .test {
            expectRawMap().should.equal(mapOf("first" to "Jimmy"))
            expectComplete()
         }
   }

   @Test
   fun `project by enriching from other services`() = runBlocking {
      val schemaStr = """
         type Symbol inherits String
         type Field1 inherits String
         type ParentType inherits String
         type ChildType inherits ParentType
         type GrandChildType inherits ChildType
         type ProvidedByService inherits String

         model ServiceData {
            @Id
            input : ChildType
            field: ProvidedByService
         }

         model Target {
               id: Symbol
               field1: Field1
               field2: ProvidedByService
         }
         model Order {
            id: Symbol
            field1: Field1
            child: ChildType
         }

         @DataSource
         service HelperService {
            operation getData( input : ChildType) : ServiceData
         }

         service OrderService {
            operation `findAll`( ) : Order[]
         }

      """.trimIndent()
      val schema = TaxiSchema.from(schemaStr)
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "getData",
         vyne.parseJsonModel(
            "ServiceData", """{
                   "field": "This is Provided By External Service"
               }"""
         )
      )

      stubService.addResponse(
         "`findAll`",
         vyne.parseJsonModel(
            "Order[]", """
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
            """
         )
      )
      val queryResult = vyne.query(
         """
         findAll {
            Order[]
         } as Target[]""".trimIndent()
      )
      queryResult.results.test {
         expectTypedObject()["field2"].value.should.equal("This is Provided By External Service")
         expectTypedObject()["field2"].value.should.equal("This is Provided By External Service")
         expectComplete()
      }

   }

   @Test
   fun `project an array of Orders to the array of CommonOrder`() = runBlocking {
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
      val noOfRecords = 100

      val (vyne, stubService) = testVyne(schema)

      stubService.addResponse("getBroker1Orders") { _, parameters ->
         parameters.should.have.size(2)
         vyne.parseJsonCollection("Broker1Order[]", generateBroker1OrdersWithTraderId(noOfRecords))
      }

      stubService.addResponse("getBroker2Orders") { _, parameters ->
         parameters.should.have.size(2)
         vyne.parseJsonCollection("Broker2Order[]", "[]")
      }
      stubService.addResponse("getUserNameFromId") { _, parameters ->
         parameters.should.have.size(1)
         val userName = when (val userId = parameters[0].second.value as String) {
            "trader0" -> "John Smith"
            "trader1" -> "Mike Brown"
            else -> TODO("Unknown userId=$userId")
         }
         listOf(vyne.parseKeyValuePair("UserName", userName))
      }

      // act
      val result = vyne.query(
         """
         findAll {
            Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30")
         } as CommonOrder[]""".trimIndent()
      )
      result.rawResults.test {
         val resultList = expectMany<Map<String, Any?>>(100)
         resultList[0].should.equal(
            mapOf(
               "id" to "broker1Order1",
               "date" to "2020-01-01",
               "traderId" to "trader1",
               "traderName" to "Mike Brown"
            )
         )
         resultList[1].should.equal(
            mapOf(
               "id" to "broker1Order1",
               "date" to "2020-01-01",
               "traderId" to "trader0",
               "traderName" to "John Smith"
            )
         )
         expectComplete()
      }
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
   fun `project to CommonOrder and resolve Enum synonyms and Instruments`() = runBlocking {
      // prepare
      val schema = """
// Primitives
type alias OrderId as String
type alias OrderDate as Date
type InstrumentId inherits String
type InstrumentDescription inherits String

// common types
type Instrument {
   @Id
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
      val noOfRecords = 10
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getBroker1Orders") { _, parameters ->
         parameters.should.have.size(2)
         TypedInstance.from(vyne.type("Broker1Order[]"), generateBroker1Orders(noOfRecords), vyne.schema) as List<TypedInstance>
      }
      stubService.addResponse("getInstrument") { _, parameters ->

         parameters.should.have.size(1)
         val instrumentId = parameters[0].second.value as String
         val (instrumentDescription, instrumentType) = when (instrumentId) {
            "instrument0" -> "UST 2Y5Y10Y" to "Type1"
            "instrument1" -> "GBP/USD 1Year Swap" to "Type2"
            else -> TODO("Unknown userId=$instrumentId")
         }

         val instrumentResponse =
            """{"id":"$instrumentId", "description": "$instrumentDescription", "instrument_type": "$instrumentType"}"""
         listOf(vyne.parseJson("Instrument", instrumentResponse))
      }

      // act
      val result =
         vyne.query("""findAll { Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30") } as CommonOrder[]""".trimIndent())

      // assert
      result.rawResults.test {
         val resultList = expectManyRawMaps(noOfRecords)
         resultList[0].should.equal(
            mapOf(
               "id" to "broker1Order0",
               "date" to "2020-01-01",
               "direction" to "sell",
               "instrument" to mapOf(
                  "id" to "instrument0",
                  "description" to "UST 2Y5Y10Y",
                  "instrument_type" to "Type1"
               ),
               "orderInstrumentType" to "OrderInstrumentType1"
            )
         )
         resultList[1].should.equal(
            mapOf(
               "id" to "broker1Order1",
               "date" to "2020-01-01",
               "direction" to "sell",
               "instrument" to mapOf(
                  "id" to "instrument1",
                  "description" to "GBP/USD 1Year Swap",
                  "instrument_type" to "Type2"
               ),
               "orderInstrumentType" to "OrderInstrumentType2"
            )
         )
         expectComplete()
      }
   }

   @Test
   @Ignore
   fun `project to CommonOrder with Trades`() = runBlocking {
      // TODO confirm how the mappings should look like
      val noOfRecords = 100
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
      stubService.addResponse("getBroker1Orders") { _, parameters ->
         parameters.should.have.size(2)
         vyne.parseJsonCollection("Broker1Order[]", orders)
      }

      stubService.addResponse("getBroker1Trades") { _, parameters ->
         parameters.should.have.size(1)
         vyne.parseJsonCollection("Broker1Trade[]", trades)
      }

      var getBroker1TradesForOrderIdsInvocationCount = 0
      stubService.addResponse("getBroker1TradesForOrderIds") { _, parameters ->

         parameters.should.have.size(1)
         val orderIds = parameters[0].second.value as List<TypedValue>
         val buf = StringBuilder("[")
         val json = orderIds.mapIndexed { index, typedValue ->
            generateBroker1Trades(typedValue.value as String, index)
         }.joinToString(",", prefix = "[", postfix = "]")
         getBroker1TradesForOrderIdsInvocationCount++
         vyne.parseJsonCollection("Broker1Trade[]", json)
      }

      var findOneByOrderIdInvocationCount = 0
      stubService.addResponse("findOneByOrderId") { _, parameters ->
         parameters.should.have.size(1)
         val orderId = parameters[0].second.value as String
         findOneByOrderIdInvocationCount++
         listOf(
            vyne.parseJsonModel(
               "Broker1Trade", """
               {
                  "broker1OrderID" : "broker1Order$orderId",
                  "broker1TradeID" : "trade_id_$orderId",
                  "broker1Price"   : 10.1,
                  "broker1TradeNo": "trade_no_$orderId"
               }
            """.trimIndent()
            )
         )
      }

      // act
      val result =
         vyne.query("""findAll { Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30") } as CommonOrder[]""".trimIndent())


      // assert
      expect(result.isFullyResolved).to.be.`true`
      result.rawResults.test(Duration.INFINITE) {
         val resultList = expectManyRawMaps(noOfRecords)
         resultList.forEachIndexed { index, result ->
            result.should.equal(
               mapOf(
                  "id" to "broker1Order$index",
                  "date" to "2020-01-01",
                  "tradeNo" to "trade_no_broker1Order$index",
                  "identifierType" to "ISIN",
                  "direction" to "Direction.SELL"
               )
            )
         }
         expectComplete()
      }
//    ProjectonHeuristics not currently working as part of reactive refactor.
      // Need to revisit this.  LENS-527
//      findOneByOrderIdInvocationCount.should.equal(0)
//      getBroker1TradesForOrderIdsInvocationCount.should.equal(1)
   }

   @Test
   @Ignore("One-to-many not currenty supported")
   fun `One to Many Mapping Projection with a date between query`() = runBlocking {
      val (vyne, stubService) = testVyne(testSchema)
      // 1 order and 3 matching trades.
      val numberOfOrders = 1
      val numberOfCorrespondingTrades = 3
      // Generate 2 orders/
      // 1 order will have 3 corresponding trades whereas the other order has none..
      val orders = generateBroker1Orders(numberOfOrders + 1)
      stubService.addResponse("getBroker1Orders") { _, parameters ->
         parameters.should.have.size(2)
         vyne.parseJsonCollection("Broker1Order[]", orders)
      }


      var getBroker1TradesForOrderIdsInvocationCount = 0
      stubService.addResponse("getBroker1TradesForOrderIds") { _, parameters ->
         parameters.should.have.size(1)
         val orderIds = parameters[0].second.value as List<TypedValue>
         val json = (0 until numberOfCorrespondingTrades).map { index ->
            generateBroker1Trades(orderIds.first().value as String, 0, index, "10.$index")
         }.joinToString(",", prefix = "[", postfix = "]")
         getBroker1TradesForOrderIdsInvocationCount++
         vyne.parseJsonCollection("Broker1Trade[]", json)
      }

      var findOneByOrderIdInvocationCount = 0
      stubService.addResponse("findOneByOrderId") { _, parameters ->
         parameters.should.have.size(1)
         val orderId = parameters[0].second.value as String
         findOneByOrderIdInvocationCount++

         listOf(TypedNull.create(vyne.type("Broker1Trade")))
      }

      // act
      val result =
         vyne.query("""findAll { Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30") } as CommonOrder[]""".trimIndent())

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.rawObjects()
      resultList.size.should.be.equal(numberOfCorrespondingTrades + 1)
      resultList.forEachIndexed { index, typedObject ->
         if (index < 3) {
            // First three results corresponding to 1 order 3 trades setup
            typedObject.should.equal(
               mapOf(
                  "id" to "broker1Order0",
                  "date" to "2020-01-01",
                  "tradeNo" to "trade_no_$index"
               )
            )
         } else {
            //last result, corresponding to an order without a trade and hence it doesn't contain a tradeNo.
            typedObject.should.equal(
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
      Unit
   }

   @Test
   @Ignore("One-to-many not supported currently")
   fun `One to Many Mapping Projection with an Id equals query`() = runBlocking {
      val (vyne, stubService) = testVyne(testSchema)
      // 1 order and 3 matching trades.
      val numberOfOrders = 1
      val numberOfCorrespondingTrades = 3
      // Generate 2 orders/
      // 1 order will have 3 corresponding trades whereas the other order has none..
      val orders = generateBroker1Orders(numberOfOrders + 1)
      stubService.addResponse("getBroker1Orders") { _, parameters ->
         parameters.should.have.size(2)
         vyne.parseJsonCollection("Broker1Order[]", orders)
      }

      var getBroker1TradesForOrderIdsInvocationCount = 0
      stubService.addResponse("getBroker1TradesForOrderIds") { _, parameters ->
         parameters.should.have.size(1)
         val orderIds = parameters[0].second.value as List<TypedValue>
         val json = (0 until numberOfCorrespondingTrades).map { index ->
            generateBroker1Trades(orderIds.first().value as String, 0, index, "10.$index")
         }.joinToString(",", prefix = "[", postfix = "]")

         getBroker1TradesForOrderIdsInvocationCount++
         vyne.parseJsonCollection("Broker1Trade[]", json)
      }


      var findOneByOrderIdInvocationCount = 0
      stubService.addResponse("findOneByOrderId") { _, parameters ->
         parameters.should.have.size(1)
         val orderId = parameters[0].second.value as String
         findOneByOrderIdInvocationCount++

         listOf(TypedNull.create(vyne.type("Broker1Trade")))
      }

      //find by order Id and project
      stubService.addResponse("findSingleByOrderID") { _, parameters ->
         parameters.should.have.size(1)
         if (parameters.first().second.value == "broker1Order0") {
            listOf(vyne.parseJsonModel("Broker1Order", generateBroker1Order(0)))
         } else {
            listOf(vyne.parseJsonModel("Broker1Order", "{}"))
         }
      }

      val findByOrderIdResult =
         vyne.query("""findAll { Order (OrderId = "broker1Order0") } as CommonOrder[]""".trimIndent())

      val result = findByOrderIdResult.results.toList()
      result.should.not.be.empty
      findByOrderIdResult.typedInstances().should.have.size(numberOfCorrespondingTrades)
      Unit
   }

   @Test
   @Ignore("One-to-many not currently supported")
   fun `One to Many Mapping Projection with an Id equals query returning zero match`() = runBlocking {
      val (vyne, stubService) = testVyne(testSchema)
      // 1 order and 3 matching trades.
      val numberOfOrders = 1
      val numberOfCorrespondingTrades = 3
      // Generate 2 orders/
      // 1 order will have 3 corresponding trades whereas the other order has none..
      val orders = generateBroker1Orders(numberOfOrders + 1)
      stubService.addResponse("getBroker1Orders") { _, parameters ->
         parameters.should.have.size(2)
         vyne.parseJsonCollection("Broker1Order[]", orders)
      }

      var getBroker1TradesForOrderIdsInvocationCount = 0
      stubService.addResponse("getBroker1TradesForOrderIds") { _, parameters ->
         parameters.should.have.size(1)
         val orderIds = parameters[0].second.value as List<TypedValue>
         val json = (0 until numberOfCorrespondingTrades).map { index ->
            generateBroker1Trades(orderIds.first().value as String, 0, index, "10.$index")
         }.joinToString(",", prefix = "[", postfix = "]")
         getBroker1TradesForOrderIdsInvocationCount++
         vyne.parseJsonCollection("Broker1Trade[]", json)
      }

      var findOneByOrderIdInvocationCount = 0
      stubService.addResponse("findOneByOrderId") { _, parameters ->
         parameters.should.have.size(1)
         val orderId = parameters[0].second.value as String
         findOneByOrderIdInvocationCount++

         listOf(TypedNull.create(vyne.type("Broker1Trade")))
      }

      //find by order Id and project
      stubService.addResponse("findSingleByOrderID") { _, parameters ->
         parameters.should.have.size(1)
         if (parameters.first().second.value == "broker1Order0") {
            listOf(vyne.parseJsonModel("Broker1Order", generateBroker1Order(0)))
         } else {
            listOf(TypedNull.create(vyne.type("Broker1Order")))
//               vyne.parseJsonModel("Broker1Order", "{}")
         }
      }
      // find by a non-existing order Id and project
      val noResult = vyne.query("""findAll { Order (OrderId = "MY SPECIAL ORDER ID") } as CommonOrder[]""".trimIndent())
      noResult.typedInstances().should.be.empty
      Unit
   }

   @Test
   @Ignore("One-to-many not supported currently")
   fun `Multiple orders with same id and multiple trades with same order Id`() = runBlocking {
      val (vyne, stubService) = testVyne(testSchema)
      val numberOfCorrespondingTrades = 3
      // 2 orders (with same id) will have 3 corresponding trades
      val orders = listOf(
         generateBroker1Order(0, "2020-01-01"),
         generateBroker1Order(0, "2021-01-01")
      ).joinToString(prefix = "[", postfix = "]")
      stubService.addResponse("getBroker1Orders") { _, parameters ->
         parameters.should.have.size(2)
         vyne.parseJsonCollection("Broker1Order[]", orders)
      }

      var getBroker1TradesForOrderIdsInvocationCount = 0
      stubService.addResponse("getBroker1TradesForOrderIds") { _, parameters ->
         parameters.should.have.size(1)
         val orderIds = parameters[0].second.value as List<TypedValue>
         val json = (0 until numberOfCorrespondingTrades).map { index ->
            generateBroker1Trades(orderIds.first().value as String, 0, index, "10.$index")
         }.joinToString(prefix = "[", postfix = "]")
         getBroker1TradesForOrderIdsInvocationCount++
         vyne.parseJsonCollection("Broker1Trade[]", json)
      }

      var findOneByOrderIdInvocationCount = 0
      stubService.addResponse("findOneByOrderId") { _, parameters ->
         parameters.should.have.size(1)
         val orderId = parameters[0].second.value as String
         findOneByOrderIdInvocationCount++

         listOf(TypedNull.create(vyne.type("Broker1Trade")))
      }

      // act
      val result =
         vyne.query("""findAll { Order[] (OrderDate  >= "2000-01-01", OrderDate < "2020-12-30") } as CommonOrder[]""".trimIndent())

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.rawObjects()
      resultList.should.have.size(2 * numberOfCorrespondingTrades)
      resultList.forEachIndexed { index, resultMember ->
         if (index < numberOfCorrespondingTrades) {
            // First three results corresponding to 1 order 3 trades setup
            resultMember.should.equal(
               mapOf(
                  "id" to "broker1Order0",
                  "date" to "2020-01-01",
                  "tradeNo" to "trade_no_$index"
               )
            )
         } else {
            resultMember.should.equal(
               mapOf(
                  "id" to "broker1Order0",
                  "date" to "2021-01-01",
                  "tradeNo" to "trade_no_${index - numberOfCorrespondingTrades}"
               )
            )
         }
      }
      findOneByOrderIdInvocationCount.should.equal(0)
      getBroker1TradesForOrderIdsInvocationCount.should.equal(1)
      Unit
   }

   private fun generateBroker1Trades(
      orderId: String,
      index: Int,
      tradeId: Int? = null,
      price: String? = null
   ): String {
      val brokerTraderId = tradeId?.let { "trade_id_$it" } ?: "trade_id_$index"
      val brokerTradeNo = tradeId?.let { "trade_no_$it" } ?: "trade_no_$index"
      val brokerPrice = price ?: "10.1"
      return """
         {
            "broker1OrderID" : "$orderId",
            "broker1TradeID" :  "$brokerTraderId",
            "broker1Price"   : "$brokerPrice",
            "broker1TradeNo": "$brokerTradeNo"
         }
         """.trimMargin()
   }

   private fun generateOneBroker1TradeForEachOrder(noOfRecords: Int): String {
      val buf = StringBuilder("[")
      for (i in 0 until noOfRecords) {
         buf.append(
            """
         {
            "broker1OrderID" : "broker1Order$i",
            "broker1TradeID" : "trade_id_$i",
            "broker1Price"   : "10.1",
            "broker1TradeNo": "trade_no_$i"
         }
         """.trimMargin()
         )
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

   //
//   @Test
//   fun `missing Country does not break Client projection`() = runBlocking {
//      // prepare
//      val testSchema = """
//         model Client {
//            name : PersonName as String
//            country : CountryCode as String
//         }
//         model Country {
//             @Id
//             countryCode : CountryCode
//             countryName : CountryName as String
//         }
//         model ClientAndCountry {
//            personName : PersonName
//            countryName : CountryName
//         }
//
//         service MultipleInvocationService {
//            operation getCustomers():Client[]
//            operation getCountry(CountryCode): Country
//         }
//      """.trimIndent()
//
//      val (vyne, stubService) = testVyne(testSchema)
//      stubService.addResponse(
//         "getCustomers", vyne.parseJsonCollection(
//            "Client[]", """
//         [
//            { name : "Jimmy", country : "UK" },
//            { name : "Devrim", country : "TR" }
//         ]
//         """.trimIndent()
//         )
//      )
//
//      stubService.addResponse("getCountry") { _, parameters ->
//         val countryCode = parameters.first().second.value!!.toString()
//         if (countryCode == "UK") {
//            listOf(vyne.parseJsonModel("Country", """{"countryCode": "UK", "countryName": "United Kingdom"}"""))
//         } else {
//            listOf(TypedObject(vyne.schema.type("Country"), emptyMap(), Provided))
//         }
//      }
//
//      // act
//      val result = vyne.query("""findAll { Client[] } as ClientAndCountry[]""".trimIndent())
//
//      // assert
//      result.rawResults.test {
//         expectRawMap().should.equal(mapOf("personName" to "Jimmy", "countryName" to "United Kingdom"))
//         expectRawMap().should.equal(mapOf("personName" to "Devrim", "countryName" to null))
//         expectComplete()
//      }
//   }
//
//   @Test
//   fun `duplicate matches with same values in projection is resolved without any errors`() = runBlocking {
//      // prepare
//      val testSchema = """
//         type OrderId inherits String
//         type TraderName inherits String
//         type InstrumentId inherits String
//         type MaturityDate inherits Date
//         type TradeId inherits String
//         type InstrumentName inherits String
//         model Order {
//            orderId: OrderId
//            traderName : TraderName
//            instrumentId: InstrumentId
//         }
//         model Instrument {
//             @Id
//             instrumentId: InstrumentId
//             maturityDate: MaturityDate
//             name: InstrumentName
//         }
//         model Trade {
//            @Id
//            orderId: OrderId
//            maturityDate: MaturityDate
//            tradeId: TradeId
//         }
//
//         model Report {
//            orderId: OrderId
//            tradeId: TradeId
//            instrumentName: InstrumentName
//            maturityDate: MaturityDate
//            traderName : TraderName
//         }
//
//         service MultipleInvocationService {
//            operation getOrders(): Order[]
//            operation getTrades(orderIds: OrderId): Trade
//            operation getTrades(orderIds: OrderId[]): Trade[]
//            operation getInstrument(instrumentId: InstrumentId): Instrument
//         }
//      """.trimIndent()
//
//      val maturityDate = "2025-12-01"
//      val (vyne, stubService) = testVyne(testSchema)
//      stubService.addResponse(
//         "getOrders", vyne.parseJsonCollection(
//            "Order[]", """
//         [
//            {
//               "orderId": "orderId_0",
//               "traderName": "john",
//               "instrumentId": "Instrument_0"
//            }
//         ]
//         """.trimIndent()
//         )
//      )
//
//      stubService.addResponse(
//         "getInstrument", vyne.parseJsonModel(
//            "Instrument", """
//            {
//               "maturityDate": "$maturityDate",
//               "instrumentId": "Instrument_0",
//               "name": "2040-11-20 0.1 Bond"
//            }
//         """.trimIndent()
//         )
//      )
//
//      stubService.addResponse(
//         "getTrades", vyne.parseJsonCollection(
//            "Trade[]", """
//            [{
//               "maturityDate": "$maturityDate",
//               "orderId": "orderId_0",
//               "tradeId": "Trade_0"
//            }]
//         """.trimIndent()
//         )
//      )
//      val result = vyne.query("""findAll { Order[] } as Report[]""".trimIndent())
//      result.isFullyResolved.should.be.`true`
//      result.rawObjects().should.equal(
//         listOf(
//            mapOf(
//               "orderId" to "orderId_0",
//               "traderName" to "john",
//               "tradeId" to "Trade_0",
//               "instrumentName" to "2040-11-20 0.1 Bond",
//               "maturityDate" to maturityDate
//            )
//         )
//      )
//   }
//
//
   @Test
   fun `can use calculated fields on output models`(): Unit = runBlocking {
      val (vyne, _) = testVyne(
         """
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
      """.trimIndent()
      )
      val input = vyne.parseJsonModel("Input", """{ "qty" : 100, "value" : 2 }""", source = Provided)
      val output = vyne.from(input).build("Output").firstTypedObject()
      output["cost"].value.should.equal(200)
   }

   @Test
   fun `can use when by`(): Unit = runBlocking {
      val (vyne, _) = testVyne(
         """
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
      """.trimIndent()
      )
      val input = vyne.parseJsonModel("Input", """{ "value": 100, "str": "Percentage" }""", source = Provided)
      val result = vyne.from(input).build("SampleType")
         .firstTypedObject()
      result["priceType"].value.should.equal("Percentage")
      Unit
   }


   @Test
   fun `A service annotated with @DataSource will not be invoked twice`() = runBlocking {
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
      stubService.addResponse(
         "getCustomers", vyne.parseJsonModel(
            "Client[]", """
         [
            { name : "Jimmy", country : "UK" },
            { name : "Devrim", country : "TR" }
         ]
         """.trimIndent()
         )
      )

      stubService.addResponse("getCountry") { _, parameters ->
         getCountryInvoked = true
         val countryCode = parameters.first().second.value!!.toString()
         if (countryCode == "UK") {
            listOf(vyne.parseJsonModel("Country", """{"countryCode": "UK", "countryName": "United Kingdom"}"""))
         } else {
            listOf(TypedObject(vyne.schema.type("Country"), emptyMap(), Provided))
         }
      }

      // act
      val result = vyne.query("""findAll { Client[] } as ClientAndCountry[]""".trimIndent())
      result.rawResults.test {
         expectRawMap().should.equal(mapOf("personName" to "Jimmy", "countryName" to null))
         expectRawMap().should.equal(
            mapOf(
               "personName" to "Devrim",
               "countryName" to null
            ) // See TypedObjectFactory.build() for discussion on returning nulls
         )
         getCountryInvoked.should.be.`false`
         expectComplete()
      }
   }

   @Test
   fun `All services referenced in @DataSource will not be invoked twice`() = runBlocking {
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


         service CountryService {
           operation findByCountryCode(CountryCode): Country
         }

         @Datasource(exclude = "[[CountryService]]")
         service MultipleInvocationService {
            operation getCustomers():Client[]
            operation getCountry(CountryCode): Country
         }
      """.trimIndent()

      var getCountryInvoked = false
      val (vyne, stubService) = testVyne(testSchema)
      stubService.addResponse(
         "findByCountryCode",
         vyne.parseJsonModel(
            "Country", """
         {countryCode: "UK", countryName: "United Kingdom"}
      """.trimIndent()
         )
      )
      stubService.addResponse(
         "getCustomers", vyne.parseJsonModel(
            "Client[]", """
         [
            { name : "Jimmy", country : "UK" },
            { name : "Devrim", country : "TR" }
         ]
         """.trimIndent()
         )
      )

      stubService.addResponse("getCountry") { _, parameters ->
         getCountryInvoked = true
         val countryCode = parameters.first().second.value!!.toString()
         if (countryCode == "UK") {
            listOf(vyne.parseJsonModel("Country", """{"countryCode": "UK", "countryName": "United Kingdom"}"""))
         } else {
            listOf(TypedObject(vyne.schema.type("Country"), emptyMap(), Provided))
         }
      }

      // act
      val result = vyne.query("""findAll { Client[] } as ClientAndCountry[]""".trimIndent())
      result.rawResults
         .test {
            expectRawMap().should.equal(mapOf("personName" to "Jimmy", "countryName" to null))
            expectRawMap().should.equal(
               mapOf(
                  "personName" to "Devrim",
                  "countryName" to null
               )
            )
            getCountryInvoked.should.be.`false`
            expectComplete()
         }
   }


   @Test
   fun `should output offset  correctly`(): Unit = runBlocking {
      val (vyne, stubService) = testVyne(
         """
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
      """.trimIndent()
      )

      val inputInstant1 = "2020-08-19T13:07:09.591Z"
      val inputInstant2 = "2020-08-18T13:07:09.591Z"
      val outputInstant1 = "2020-08-19T14:07:09.591+0100"
      val outputInstant2 = "2020-08-18T14:07:09.591+0100"
      stubService.addResponse(
         "getInputData", vyne.parseJsonCollection(
            "InputModel[]", """
         [
            { "inputField": "$inputInstant1" },
            { "inputField": "$inputInstant2" }
         ]
         """.trimIndent()
         )
      )
      val result = vyne.query("""findAll { InputModel[] } as OutputModel[]""".trimIndent())
      result.rawObjects().should.equal(
         listOf(
            mapOf("myField" to outputInstant1),
            mapOf("myField" to outputInstant2)
         )
      )
   }

   @Test
   fun `should output offset correctly without any format`(): Unit = runBlocking {
      val (vyne, stubService) = testVyne(
         """
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
      """.trimIndent()
      )

      val inputInstant1 = "2020-08-19T13:07:09.591Z"
      val inputInstant2 = "2020-08-18T13:07:09.591Z"
      val outputInstant1 = "2020-08-19T14:07:09.591+01"
      val outputInstant2 = "2020-08-18T14:07:09.591+01"
      stubService.addResponse(
         "getInputData", vyne.parseJsonCollection(
            "InputModel[]", """
         [
            { "inputField": "$inputInstant1" },
            { "inputField": "$inputInstant2" }
         ]
         """.trimIndent()
         )
      )
      val result = vyne.query("""findAll { InputModel[] } as OutputModel[]""".trimIndent())
      result.rawObjects().should.be.equal(
         listOf(
            mapOf("myField" to outputInstant1),
            mapOf("myField" to outputInstant2)
         )
      )
   }

   @Test
   fun `Calculated fields should be correctly set in projected type`(): Unit = runBlocking {
      val (vyne, stubService) = testVyne(
         """
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
      """.trimIndent()
      )

      stubService.addResponse(
         "getInputData", vyne.parseJsonCollection(
            "InputModel[]", """
         [
            { "qtyFill": 200, "multiplier": 2 }
         ]
         """.trimIndent()
         )
      )
      val result = vyne.query("""findAll { InputModel[] } as OutputModel[]""".trimIndent())
      result.rawObjects().should.be.equal(
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
   fun `when calculating fields then lineage is set on output`() = runBlocking {
      val (vyne, stubs) = testVyne(
         """
            type Quantity inherits Int
            type Price inherits Int

            model Order {
               quantity : Quantity
               price : Price
            }
            model Output {
               quantity : Quantity
               price : Price
               averagePrice : Decimal by (this.price / this.quantity)
            }
            service OrderService {
               operation listOrders():Order[]
            }
         """.trimIndent()
      )
      // The below responseJson will trigger a divide-by-zero
      val responseJson = """[
         |{ "quantity" : 0 , "price" : 2 }
         |]""".trimMargin()
      stubs.addResponse(
         "listOrders", vyne.parseJsonModel(
            "Order[]", """[
         |{ "quantity" : 100 , "price" : 2 },
         |{ "quantity" : 0 , "price" : 2 }
         |]""".trimMargin()
         )
      )

      val queryResult = vyne.query("findAll { Order[] } as Output[]")
//      val resultList = queryResult.results.toList()
//      resultList.should.not.be.`null`
      val outputCollection = queryResult.results.test {
         val outputModel = expectTypedObject()
         val averagePrice = outputModel["averagePrice"]
         averagePrice.value.should.equal(0.02.toBigDecimal())
         val averagePriceDataSource = averagePrice.source as EvaluatedExpression
         averagePriceDataSource.expressionTaxi.should.equal("(this.price / this.quantity)")
         averagePriceDataSource.inputs[0].value.should.equal(2)
         averagePriceDataSource.inputs[0].source.should.equal(Provided)

         val modelWithError = expectTypedObject()
         val priceWithError = modelWithError["averagePrice"]
         priceWithError.value.should.be.`null`
         priceWithError.source.should.be.instanceof(FailedEvaluatedExpression::class.java)
         val failedExpression = priceWithError.source as FailedEvaluatedExpression
         failedExpression.errorMessage.should.equal("Division by zero")
         failedExpression.inputs[0].value.should.equal(2)
         failedExpression.inputs[1].value.should.equal(0)
         expectComplete()
      }

   }

   @Test
   fun `should project to pure anonymous type with single field`(): Unit = runBlocking {
      val (vyne, stubService) = testVyne(
         """
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
      """.trimIndent()
      )

      stubService.addResponse(
         "getInputData", vyne.parseJsonCollection(
            "InputModel[]", """
         [
            { "qtyFill": 200, "multiplier": 2, "id": "input1" },
            { "qtyFill": 200, "multiplier": 2, "id": "input2" },
            { "qtyFill": 200, "multiplier": 2, "id": "input3" }
         ]
         """.trimIndent()
         )
      )
      val result = vyne.query(
         """
            findAll {
                InputModel[]
              } as {
                 id
               }[]
            """.trimIndent()
      )

      result.rawObjects().should.be.equal(
         listOf(
            mapOf("id" to "input1"), mapOf("id" to "input2"), mapOf("id" to "input3")
         )
      )
   }

   @Test
   fun `should project to pure anonymous type with multiple fields`() = runBlocking {
      val (vyne, stubService) = testVyne(
         """
         type QtyFill inherits Decimal
         type UnitMultiplier inherits Decimal
         type FilledNotional inherits Decimal
         type InputId inherits String
         type TraderId inherits String
         type TraderName inherits String
         type TraderSurname inherits String

         model InputModel {
           multiplier: UnitMultiplier by default(2)
           qtyFill: QtyFill
           id: InputId
           traderId: TraderId
         }

         model OutputModel {
            qtyHit : QtyFill?
            unitMultiplier: UnitMultiplier?
            filledNotional : FilledNotional?  by (this.qtyHit * this.unitMultiplier)
         }

          model TraderInfo {
            @Id
            traderId: TraderId
            traderName: TraderName
            traderSurname: TraderSurname
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }

         service TraderService {
            operation getTrader(TraderId): TraderInfo
         }
      """.trimIndent()
      )

      stubService.addResponse(
         "getInputData", vyne.parseJsonCollection(
            "InputModel[]", """
         [
            { "qtyFill": 200, "multiplier": 1, "id": "input1", "traderId": "tId1" },
            { "qtyFill": 200, "multiplier": 2, "id": "input2", "traderId": "tId2" },
            { "qtyFill": 200, "multiplier": 3, "id": "input3", "traderId": "tId3" }
         ]
         """.trimIndent()
         )
      )

      stubService.addResponse("getTrader") { _, parameters ->
         when (parameters.first().second.value) {
            "tId1" -> listOf(
               vyne.parseJsonModel(
                  "TraderInfo",
                  """{"traderId": "tId1", "traderName": "Butch", "traderSurname": "Cassidy"}"""
               )
            )
            "tId2" -> listOf(
               vyne.parseJsonModel(
                  "TraderInfo",
                  """{"traderId": "tId2", "traderName": "Sundance", "traderSurname": "Kidd"}"""
               )
            )
            "tId3" -> listOf(
               vyne.parseJsonModel(
                  "TraderInfo",
                  """{"traderId": "tId3", "traderName": "Travis", "traderSurname": "Bickle"}"""
               )
            )
            else -> listOf(TypedNull.create(vyne.type("TraderInfo")))

         }
      }

      val result = vyne.query(
         """
            findAll {
                InputModel[]
              } as {
                 id
                 multiplier: UnitMultiplier
                 traderName: TraderName by (this.traderId)
               }[]
            """.trimIndent()
      )

      result.rawResults.test {
         expectRawMapsToEqual(
            listOf(
               mapOf("id" to "input1", "multiplier" to BigDecimal("1"), "traderName" to "Butch"),
               mapOf("id" to "input2", "multiplier" to BigDecimal("2"), "traderName" to "Sundance"),
               mapOf("id" to "input3", "multiplier" to BigDecimal("3"), "traderName" to "Travis")
            )
         )
         expectComplete()
      }
   }

   @Test
   fun `should project to anonymous type extending discovery type`(): Unit = runBlocking {
      val (vyne, stubService) = testVyne(
         """
         type QtyFill inherits Decimal
         type UnitMultiplier inherits Decimal
         type FilledNotional inherits Decimal
         type InputId inherits String
         type TraderId inherits String
         type TraderName inherits String
         type TraderSurname inherits String

         model InputModel {
           multiplier: UnitMultiplier by default(2)
           qtyFill: QtyFill
           id: InputId
         }

         model OutputModel {
            qtyHit : QtyFill?
            unitMultiplier: UnitMultiplier?
            filledNotional : FilledNotional?  by (this.qtyHit * this.unitMultiplier)
            traderId: TraderId by default("id1")
         }

         model TraderInfo {
            @Id
            traderId: TraderId
            traderName: TraderName
            traderSurname: TraderSurname
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }

         service TraderService {
            operation getTrader(TraderId): TraderInfo
         }
      """.trimIndent()
      )

      stubService.addResponse(
         "getInputData", vyne.parseJsonCollection(
            "InputModel[]", """
         [
            { "qtyFill": 200, "multiplier": 1, "id": "input1" },
            { "qtyFill": 200, "multiplier": 2, "id": "input2" },
            { "qtyFill": 200, "multiplier": 3, "id": "input3" }
         ]
         """.trimIndent()
         )
      )

      stubService.addResponse(
         "getTrader", vyne.parseJsonModel(
            "TraderInfo",
            """
         {
            "traderId": "id1",
            "traderName": "John",
            "traderSurname" : "Doe"
         }
      """.trimIndent()
         )
      )
      val result =
         vyne.query(
            """
            findAll {
                InputModel[]
              } as OutputModel {
                 inputId: InputId
                 traderName: TraderName by (this.traderId)
               }[]
            """.trimIndent()
         )

      result.rawObjects().should.be.equal(
         listOf(
            mapOf(
               "qtyHit" to BigDecimal("200"),
               "unitMultiplier" to BigDecimal("1"),
               "filledNotional" to BigDecimal("200"),
               "traderId" to "id1",
               "inputId" to "input1",
               "traderName" to "John"
            ),
            mapOf(
               "qtyHit" to BigDecimal("200"),
               "unitMultiplier" to BigDecimal("2"),
               "filledNotional" to BigDecimal("400"),
               "traderId" to "id1",
               "inputId" to "input2",
               "traderName" to "John"
            ),
            mapOf(
               "qtyHit" to BigDecimal("200"),
               "unitMultiplier" to BigDecimal("3"),
               "filledNotional" to BigDecimal("600"),
               "traderId" to "id1",
               "inputId" to "input3",
               "traderName" to "John"
            )
         )
      )
   }

   @Test
   fun `should project to anonymous extending the projectiont target type and containing an anonymously typed field`(): Unit =
      runBlocking {
         val (vyne, stubService) = testVyne(
            """
         type QtyFill inherits Decimal
         type UnitMultiplier inherits Decimal
         type FilledNotional inherits Decimal
         type InputId inherits String
         type TraderId inherits String
         type TraderName inherits String
         type TraderSurname inherits String

         model InputModel {
           multiplier: UnitMultiplier by default(2)
           qtyFill: QtyFill
           id: InputId
         }

         model OutputModel {
            qtyHit : QtyFill?
            unitMultiplier: UnitMultiplier?
            filledNotional : FilledNotional?  by (this.qtyHit * this.unitMultiplier)
            traderId: TraderId by default("id1")
         }

         model TraderInfo {
            @Id
            traderId: TraderId
            traderName: TraderName
            traderSurname: TraderSurname
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }

         service TraderService {
            operation getTrader(TraderId): TraderInfo
         }
      """.trimIndent()
         )

         stubService.addResponse(
            "getInputData", vyne.parseJsonCollection(
               "InputModel[]", """
         [
            { "qtyFill": 200, "multiplier": 1, "id": "input1" },
            { "qtyFill": 200, "multiplier": 2, "id": "input2" },
            { "qtyFill": 200, "multiplier": 3, "id": "input3" }
         ]
         """.trimIndent()
            )
         )

         stubService.addResponse(
            "getTrader", vyne.parseJsonModel(
               "TraderInfo",
               """
         {
            "traderId": "id1",
            "traderName": "John",
            "traderSurname" : "Doe"
         }
      """.trimIndent()
            )
         )
         val result =
            vyne.query(
               """
            findAll {
                InputModel[]
              } as OutputModel {
                 inputId: InputId
                 trader: {
                    name: TraderName
                    surname: TraderSurname
                 } by (this.traderId)
               }[]
            """.trimIndent()
            )

         result.rawObjects().should.be.equal(
            listOf(
               mapOf(
                  "qtyHit" to BigDecimal("200"),
                  "unitMultiplier" to BigDecimal("1"),
                  "filledNotional" to BigDecimal("200"),
                  "traderId" to "id1",
                  "inputId" to "input1",
                  "trader" to mapOf("name" to "John", "surname" to "Doe")
               ),
               mapOf(
                  "qtyHit" to BigDecimal("200"),
                  "unitMultiplier" to BigDecimal("2"),
                  "filledNotional" to BigDecimal("400"),
                  "traderId" to "id1",
                  "inputId" to "input2",
                  "trader" to mapOf("name" to "John", "surname" to "Doe")
               ),
               mapOf(
                  "qtyHit" to BigDecimal("200"),
                  "unitMultiplier" to BigDecimal("3"),
                  "filledNotional" to BigDecimal("600"),
                  "traderId" to "id1",
                  "inputId" to "input3",
                  "trader" to mapOf("name" to "John", "surname" to "Doe")
               )
            )
         )
      }

   @Test
   fun `should project to anonymous type containing an anonymously typed field`(): Unit = runBlocking {
      val (vyne, stubService) = testVyne(
         """
         type QtyFill inherits Decimal
         type UnitMultiplier inherits Decimal
         type FilledNotional inherits Decimal
         type InputId inherits String
         type TraderId inherits String
         type TraderName inherits String
         type TraderSurname inherits String

         model InputModel {
           multiplier: UnitMultiplier by default(2)
           qtyFill: QtyFill
           id: InputId
           traderId: TraderId
         }

         model OutputModel {
            qtyHit : QtyFill?
            unitMultiplier: UnitMultiplier?
            filledNotional : FilledNotional?  by (this.qtyHit * this.unitMultiplier)
         }

          model TraderInfo {
            @Id
            traderId: TraderId
            traderName: TraderName
            traderSurname: TraderSurname
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }

         service TraderService {
            operation getTrader(TraderId): TraderInfo
         }
      """.trimIndent()
      )

      stubService.addResponse(
         "getInputData", vyne.parseJsonModel(
            "InputModel[]", """
         [
            { "qtyFill": 200, "multiplier": 1, "id": "input1", "traderId": "tId1" },
            { "qtyFill": 200, "multiplier": 2, "id": "input2", "traderId": "tId2" },
            { "qtyFill": 200, "multiplier": 3, "id": "input3", "traderId": "tId3" }
         ]
         """.trimIndent()
         )
      )

      stubService.addResponse("getTrader") { _, parameters ->
         when (parameters.first().second.value) {
            "tId1" -> listOf(
               vyne.parseJsonModel(
                  "TraderInfo",
                  """{"traderId": "tId1", "traderName": "Butch", "traderSurname": "Cassidy"}"""
               )
            )
            "tId2" -> listOf(
               vyne.parseJsonModel(
                  "TraderInfo",
                  """{"traderId": "tId2", "traderName": "Sundance", "traderSurname": "Kidd"}"""
               )
            )
            "tId3" -> listOf(
               vyne.parseJsonModel(
                  "TraderInfo",
                  """{"traderId": "tId3", "traderName": "Travis", "traderSurname": "Bickle"}"""
               )
            )
            else -> listOf(TypedNull.create(vyne.type("TraderInfo")))

         }
      }

      val result = vyne.query(
         """
            findAll {
                InputModel[]
              } as {
                 id
                 multiplier: UnitMultiplier
                 trader: {
                    name: TraderName
                    surname: TraderSurname
                 }  by (this.traderId)
               }[]
            """.trimIndent()
      )

      result.rawObjects().should.be.equal(
         listOf(
            mapOf(
               "id" to "input1",
               "multiplier" to BigDecimal("1"),
               "trader" to mapOf("name" to "Butch", "surname" to "Cassidy")
            ),
            mapOf(
               "id" to "input2",
               "multiplier" to BigDecimal("2"),
               "trader" to mapOf("name" to "Sundance", "surname" to "Kidd")
            ),
            mapOf(
               "id" to "input3",
               "multiplier" to BigDecimal("3"),
               "trader" to mapOf("name" to "Travis", "surname" to "Bickle")
            )
         )
      )
   }

   @Test
   fun `avoid recursive parameter discovery`(): Unit = runBlocking {
      val (vyne, stubService) = testVyne(
         """
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

         // The request contains a parameter that is present on the response.
         // Therefore, in order to construct the request, the response can be invoked.
         // This leads to circular logic, which causes a stack overflow.
         // This test asserts that behaviour is prevented.
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
      """.trimIndent()
      )

      stubService.addResponse(
         "getInputData", vyne.parseJsonCollection(
            "InputModel[]", """
         [
            {  "id": "input1", "ric": "ric1" },
            {  "id": "input2" },
            {  "id": "input3" }
         ]
         """.trimIndent()
         )
      )
      val result =
         vyne.query(
            """
            findAll {
                InputModel[]
              } as OutputModel []
            """.trimIndent()
         )

      result.rawObjects().should.be.equal(
         listOf(
            mapOf("isin" to null),
            mapOf("isin" to null),
            mapOf("isin" to null)
         )
      )
   }

   @Test
   fun `invalid post operation caching`(): Unit = runBlocking {
      val (vyne, stubService) = testVyne(
         """
         type Isin inherits String
         type Ric inherits String
         type InstrumentIdentifierType inherits String
         type InputId inherits String


         model InputModel {
           id: InputId
           ric : Ric?
           instrumentType: InstrumentIdentifierType? by default("Ric")
         }

         model OutputModel {
            isin: Isin
         }

         parameter model InstrumentReferenceRequest {
             Identifier : Ric?
             IdentifierType: InstrumentIdentifierType?
         }

         parameter model InstrumentReferenceResponse {
             isin: Isin
         }

         @Datasource
         service MultipleInvocationService {
            operation getInputData(): InputModel[]
         }

         service InstrumentService {
             operation getInstrumentFromRic( @RequestBody request:InstrumentReferenceRequest) :  InstrumentReferenceResponse
         }
      """.trimIndent()
      )

      stubService.addResponse(
         "getInputData", vyne.parseJsonCollection(
            "InputModel[]", """
         [
            {  "id": "input1", "ric": "ric1", "instrumentType": "ric" },
            {  "id": "input2", "ric": "ric2", "instrumentType": "ric" },
            {  "id": "input3", "ric": "ric3", "instrumentType": "ric" }
         ]
         """.trimIndent()
         )
      )

      stubService.addResponse("getInstrumentFromRic") { _, parameters ->
         val isinValue = (parameters.first().second as TypedObject).value.values.map { it.value }.joinToString("_")
         listOf(
            vyne.parseJsonModel(
               "InstrumentReferenceResponse", """
             {"isin": "$isinValue"}
          """.trimIndent()
            )
         )
      }

      val result =
         vyne.query(
            """
            findAll {
                InputModel[]
              } as OutputModel []
            """.trimIndent()
         )

      result.rawObjects().should.be.equal(
         listOf(
            mapOf("isin" to "ric1_ric"),
            mapOf("isin" to "ric2_ric"),
            mapOf("isin" to "ric3_ric")
         )
      )

   }

   @Test
   fun `If Vyne is enriching an entity, and a model returned from a service defines an Id field, then Vyne will only invoke that service the input parameter identifies the output model`(): Unit =
      runBlocking {
         val testSchema = """
         type UserId inherits String
         type TradeId inherits String
         type Isin inherits String
         type TradePrice inherits Decimal
         type TradeDate inherits Date

         model Trade {
           salesPersonId: UserId
           @Id
           tradeId: TradeId
           isin: Isin
           tradePrice: TradePrice
         }

          model Input {
            userId: UserId
            tradeId: TradeId
          }

          model Report {
             tradePrice: TradePrice
             tradeDate: TradeDate
          }

         @Datasource
         service InputService {
            operation `findAll`(): Input[]
         }

         service DataService {
             operation findLatestTradeForSalesPerson(UserId) : Trade
             operation findTrade(TradeId) : Trade
         }


      """.trimIndent()
         val (vyne, stubService) = testVyne(testSchema)
         stubService.addResponse(
            "`findAll`", vyne.parseJsonCollection(
               "Input[]", """
         [
            { userId : "userX",  tradeId: "InstrumentX" }
         ]
         """.trimIndent()
            )
         )

         stubService.addResponse("findLatestTradeForSalesPerson") { _, parameters ->
            fail("Should not be invoked")
         }


         var findTradeInvoked = false
         stubService.addResponse("findTrade") { _, _ ->
            findTradeInvoked = true
            throw IllegalArgumentException()
         }

         // act
         val result = vyne.query("""findAll { Input[] } as Report[]""".trimIndent())

         // assert
         result.rawObjects().should.be.equal(
            listOf(
               mapOf("tradePrice" to null, "tradeDate" to null)
            )
         )
         findTradeInvoked.should.be.`true`
      }

   @Test
   fun `If Vyne is enriching an entity, and a model returned from a service does not define an Id field, then Vyne will use any possible path to discover the inputs to call the service`(): Unit =
      runBlocking {
         val testSchema = """
         type ProductId inherits String
         type AssetClass inherits String
         type Isin inherits String
         type OrderId inherits String
         parameter model IsinDiscoveryRequest {
             productId : ProductId?
             assetClass : AssetClass?
         }
         model IsinDiscoveryResult {
             isin : Isin
         }

         model Input {
             orderId: OrderId
             productId : ProductId?
             assetClass : AssetClass?
         }

         model Output {
            orderId: OrderId
            isin : Isin
         }

         @Datasource
         service InputService {
            operation `findAll`(): Input[]
         }

         service DataService {
            operation lookupIsin(IsinDiscoveryRequest):IsinDiscoveryResult
         }

      """.trimIndent()

         val (vyne, stubService) = testVyne(testSchema)
         stubService.addResponse(
            "`findAll`", vyne.parseJsonCollection(
               "Input[]", """
         [
            { orderId : "OrderX",  productId: "ProductX", assetClass: "AssetClassX" }
         ]
         """.trimIndent()
            )
         )

         stubService.addResponse(
            "lookupIsin", vyne.parseJsonModel(
               "IsinDiscoveryResult", """
            { isin : "Isin1" }
         """.trimIndent()
            )
         )

         val result = vyne.query("""findAll { Input[] } as Output[]""".trimIndent())

         result.rawObjects().should.be.equal(
            listOf(
               mapOf("orderId" to "OrderX", "isin" to "Isin1")
            )
         )

      }

   @Test
   fun `When an object has multiple independent fields that identify it, all these fields can be used for enrichment`() =
      runBlocking {
         val testSchema = """
         type UserId inherits String
         type Type1TradeId inherits String
         type Type2TradeId inherits String
         type Isin inherits String
         type TradePrice inherits Decimal
         type TradeDate inherits Date

         model Trade {
           salesPersonId: UserId
           @Id
           tradeId1: Type1TradeId
           @Id
           tradeId2: Type2TradeId
           isin: Isin
           tradePrice: TradePrice
         }

          model Input {
            userId: UserId
            tradeId1: Type1TradeId?
            tradeId2: Type2TradeId?
          }

          model Report {
             tradePrice: TradePrice
             tradeDate: TradeDate
          }

         @Datasource
         service InputService {
            operation `findAll`(): Input[]
         }

         service DataService {
             operation findLatestTradeForSalesPerson(UserId) : Trade
             operation findTradeByType1Id(Type1TradeId) : Trade
             operation findTradeByType2Id(Type2TradeId) : Trade
         }


      """.trimIndent()
         val (vyne, stubService) = testVyne(testSchema)
         stubService.addResponse(
            "`findAll`", vyne.parseJsonCollection(
               "Input[]", """
         [
            { userId : "userX",  tradeId1: "InstrumentX" },
            { userId : "userX",  tradeId2: "InstrumentY" }
         ]
         """.trimIndent()
            )
         )

         stubService.addResponse("findLatestTradeForSalesPerson") { _, _ ->
            fail("Should not be invoked")
         }

         var findTradeByType1IdInvoked = false
         stubService.addResponse("findTradeByType1Id") { _, _ ->
            findTradeByType1IdInvoked = true
            throw IllegalArgumentException()
         }

         var findTradeByType2IdInvoked = false
         stubService.addResponse("findTradeByType2Id") { _, _ ->
            findTradeByType2IdInvoked = true
            throw IllegalArgumentException()
         }

         // act
         val result = vyne.query("""findAll { Input[] } as Report[]""".trimIndent())

         // assert
         result.rawObjects().should.be.equal(
            listOf(
               mapOf("tradePrice" to null, "tradeDate" to null),
               mapOf("tradePrice" to null, "tradeDate" to null)
            )
         )
         findTradeByType1IdInvoked.should.be.`true`
         findTradeByType2IdInvoked.should.be.`true`
      }

   @Test
   fun `when an enum synonym is used the lineage is still captured correctly`():Unit = runBlocking{
      val (vyne,stub) = testVyne("""
         enum CountryCode {
            NZ,
            AUS
         }
         enum Country {
            NewZealand synonym of CountryCode.NZ,
            Australia synonym of CountryCode.AUS
         }
         model Person {
            name : FirstName inherits String
            country : CountryCode
         }
         service PeopleService {
            operation listPeople():Person[]
         }
      """)
      val people = TypedInstance.from(vyne.type("Person[]"), """[
         |{ "name" : "Mike" , "country" : "AUS" },
         |{ "name" : "Marty", "country" : "NZ" }]
      """.trimMargin(), vyne.schema, source = Provided)
      stub.addResponse("listPeople", people, modifyDataSource = true)
      val results = vyne.query("""findAll { Person[] } as {
         | name : FirstName
         | country : Country
         | }[]
      """.trimMargin())
         .typedObjects()
      val first = results[0]
      val countrySource = first["country"].source as MappedSynonym
      val remoteSource = countrySource.source.source as OperationResult
      remoteSource.remoteCall.operationQualifiedName.toString().should.equal("PeopleService@@listPeople")
   }

   @Test
   fun concurrency_test(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type DirectorName inherits String
         type ReleaseYear inherits Int
         model Actor {
            @Id actorId : ActorId inherits String
            name : ActorName inherits String
         }
         model Movie {
            @Id movieId : MovieId inherits String
            title : MovieTitle inherits String
            starring : ActorId
         }
         model OutputModel {
            @Id movieId : MovieId inherits String
            title : MovieTitle inherits String
            starring : ActorName
            director : DirectorName
            releaseYear : ReleaseYear
         }
         service Services {
            operation findAllMovies():Movie[]
            operation findActor(ActorId):Actor
         }
      """.trimIndent()
      )
      stub.addResponseFlow("findActor") { remoteOperation, params ->
         val actorId = params[0].second.value as String
         flow {
//            kotlinx.coroutines.delay(500)
            val actor = TypedInstance.from(
               vyne.type("Actor"),
               """{ "actorId" : ${actorId.quoted()} , "name" : "Tom Cruise's Clone #$actorId" } """,
               vyne.schema,
               source = Provided
            )
            emit(actor)
         }
      }
      val movieCount = 500
      stub.addResponse("findAllMovies") { _, params ->
         val movies = (0 until movieCount).map { index ->
            val movie = mapOf(
               "movieId" to index.toString(),
               "title" to "Mission Impossible $index",
               "starring" to index.toString()
            )
            vyne.parseJsonModel("Movie", jacksonObjectMapper().writeValueAsString(movie))
         }
         movies
      }

      val start = Stopwatch.createStarted()
      var summary: StrategyPerformanceProfiler.SearchStrategySummary? = null
      val f = Benchmark.benchmark("run concurrency test with $movieCount", warmup = 5, iterations = 5) {
         runBlocking {
            val result = vyne.query("findAll { Movie[] } as OutputModel[]")
               .results.toList()
            result.should.have.size(movieCount)
            val duration = start.elapsed(TimeUnit.MILLISECONDS)
            summary = StrategyPerformanceProfiler.summarizeAndReset()
         }
      }
      log().warn("Test completed: $summary")

   }
}
