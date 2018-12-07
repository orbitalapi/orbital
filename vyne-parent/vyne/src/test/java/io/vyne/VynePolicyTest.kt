package io.vyne

import com.winterbe.expekt.expect
import io.vyne.models.json.addKeyValuePair
import io.vyne.models.json.parseJsonModel
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class VynePolicyTest {
   val taxiDef = """
namespace test {
    type TradingDesk {
        deskId : DeskId as String
    }
    type Trade {
        desk : DeskId
        counterParty : CounterPartyId as String
        amount : TradeAmount as Decimal
    }
    type alias Group as String
    type UserAuthorization {
        groups : Groups as Group[]
    }

   type alias SessionToken as String
   type User {
      userId : UserId as String
      userName : UserName as String
      auth : UserAuthorization
      deskId : DeskId as String
   }
   type TraderConfig {
      userName : UserName
      deskId : DeskId
   }
   service UserService {
      operation tokenToUserId(SessionToken):UserId
      operation findUser(UserId):User
      operation getConfig(UserName):TraderConfig
   }
   service TradeService {
      read operation listTrades():Trade[]

   }

    policy TradeDeskPolicy against Trade {
        read external {
            case caller.DeskId = this.DeskId -> permit
            case caller.Groups = anyOf("ADMIN","COMPLIANCE") -> permit
            case caller.DeskId != this.DeskId -> process using MaskTradeDetailsProcessor
            else -> deny
        }
        read internal {
            permit
        }
        write {
            case caller.DeskId = this.DeskId -> permit
            case caller.Groups = anyOf("ADMIN","COMPLIANCE") -> permit
            case caller.DeskId != this.DeskId -> process using MaskTradeDetailsProcessor
            else -> deny
        }
    }
}
   """.trimIndent()
   val schema = TaxiSchema.from(taxiDef)
   @Test
   fun canRetrievePoliciesForType() {
      val (vyne, stubService) = testVyne(schema)
      val type = vyne.getType("test.Trade")
      expect(vyne.getPolicy(type)).not.`null`
   }

   @Test
   fun loadsPoliciesForDataType() {

      val (vyne, stubService) = testVyne(schema)
      vyne.addKeyValuePair("test.SessionToken", "aabbcc")
      stubService.addResponse("listTrades", vyne.parseJsonModel("test.Trade[]", """
 [{
   "deskId" : "desk1",
    "tradeAmount" : 300
 },
 {
   "deskId" : "desk2",
   "tradeAmount" : 500
 }]
      """.trimIndent()))

      val result = vyne.query().find("test.Trade[]")
      TODO()
   }
}
