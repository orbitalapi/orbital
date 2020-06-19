package io.vyne

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.TypedInstance
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.parseKeyValuePair
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import org.junit.Test


class VyneProjectionTest {



   @Test
   fun `project array of Orders to array of IMadOrders`() {
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
            return vyne.addJsonModel("Broker1Order[]", generateBroker1Orders(noOfRecords))
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

   private fun generateBroker1Orders(noOfRecords: Int): String {
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

}

