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

model IMadOrder {
   id: OrderId
   date: OrderDate
   traderId: UserId
   traderName: UserName
}

model Order {
}
type HpcOrder inherits Order {
   hpcID: OrderId
   hpcDate: OrderDate
   traderId: UserId
}
type IonOrder inherits Order {
   ionID: OrderId
   ionDate: OrderDate
}

// operations
service HpcService {
   operation getHpcOrders( start : OrderDate, end : OrderDate) : HpcOrder[] (OrderDate >= start, OrderDate < end)
}
service IonService {
   operation getIonOrders( start : OrderDate, end : OrderDate) : IonOrder[] (OrderDate >= start, OrderDate < end)
}

service UserService {
   operation getUserNameFromId(userId: UserId):UserName
}

""".trimIndent()
      val noOfRecords = 10_000

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getHpcOrders", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.addJsonModel("HpcOrder[]", generateHpcOrders(noOfRecords))
         }
      })
      stubService.addResponse("getIonOrders", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            return vyne.addJsonModel("IonOrder[]", "[]")
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
         } as IMadOrder[]""".trimIndent())

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      resultList.size.should.be.equal(noOfRecords)
      resultList[0].should.equal(
         mapOf(
            Pair("id", "hpcOrder1"),
            Pair("date", "2020-01-01"),
            Pair("traderId", "trader1"),
            Pair("traderName", "Mike Brown"))
      )
      resultList[1].should.equal(
         mapOf(
            Pair("id", "hpcOrder1"),
            Pair("date", "2020-01-01"),
            Pair("traderId", "trader0"),
            Pair("traderName", "John Smith"))
      )
   }

   private fun generateHpcOrders(noOfRecords: Int): String {
      val buf = StringBuilder()
      buf.append("[")
      for (i in 1..noOfRecords) {
         buf.append("""{ "hpcID" : "hpcOrder1", "hpcDate" : "2020-01-01", "traderId" : "trader${i%2}"}""")
         if (i < noOfRecords) {
            buf.append(",")
         }
      }
      buf.append("]")
      return buf.toString()
   }

}

