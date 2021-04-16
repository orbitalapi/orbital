package io.vyne

import app.cash.turbine.test
import com.winterbe.expekt.expect
import io.vyne.models.*
import io.vyne.models.json.addKeyValuePair
import io.vyne.models.json.parseJsonModel
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class VynePolicyTest {

   // alt: process using SomeProcessor
   fun schema(deskMismatchPolicyBehaviour: String = "filter"): TaxiSchema {

      val taxiDef = """
namespace test {
    type TradingDesk {
        deskId : DeskId as String
    }
    // Dummy type for testing nested filtering
    type TradeWrapper {
        wrapperText : String
        trade : Trade
    }
    type Trade {
        id : TradeId as Int
//        deskId : DeskId
        counterParty : CounterPartyId as String
        amount : TradeAmount as Decimal
    }
    type alias Group as String
    type UserAuthorization {
        groups : Groups as Group[]
    }
    type Client {
      id : CounterPartyId
      deskId : ClientDeskId as DeskId
    }

   type alias SessionToken as String
   type User {
      userId : UserId as String
      userName : UserName as String
      auth : UserAuthorization
      deskId : DeskId as String?
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
      read operation getTrade(TradeId):Trade
      read operation getTradeWrapper(TradeId):TradeWrapper
   }

   service ClientService {
      operation findClient(CounterPartyId):Client
   }

    policy TradeDeskPolicy against Trade {
        read external {
            case caller.DeskId = this.ClientDeskId -> permit
            case caller.Groups in ["ADMIN","COMPLIANCE"] -> permit
            case caller.DeskId = null -> filter
            case caller.DeskId != this.ClientDeskId -> $deskMismatchPolicyBehaviour
            else -> filter
        }
        read internal {
            permit
        }
        write {
            case caller.DeskId = this.ClientDeskId -> permit
            case caller.Groups in ["ADMIN","COMPLIANCE"] -> permit
            case caller.DeskId != this.ClientDeskId -> $deskMismatchPolicyBehaviour
            else -> filter
        }
    }
}
   """.trimIndent()
      return TaxiSchema.from(taxiDef)
   }


   @Test
   fun canRetrievePoliciesForType() {
      val (vyne, _) = testVyne(schema())
      val type = vyne.getType("test.Trade")
      expect(vyne.getPolicy(type)).not.`null`
   }

   private val desk1Client = """
   {
      "id" : "desk1Client",
      "deskId" : "desk1"
   }
   """.trimIndent()
   private val desk2Client = """
   {
      "id" : "desk2Client",
      "deskId" : "desk2"
   }
   """.trimIndent()
   // nonTraderUser doesnt have a deskId
   private val nonTraderUser = """
    {
         "userId" : "supportGuy1",
         "userName" : "Sean Support",
         "deskId" : null,
         "auth" : {
            "groups" : ["IT-SUPPORT"]
         }
    }

   """.trimIndent()
   private val traderUser = """
    {
         "userId" : "jimmy123",
         "userName" : "Jimmy Trader",
         "deskId" : "desk1",
         "auth" : {
            "groups" : ["TRADERS"]
         }
    }
         """.trimIndent()

   private val traderFromWrongDesk = """
    {
         "userId" : "jack123",
         "userName" : "Jack Trader",
         "deskId" : "desk2",
         "auth" : {
            "groups" : ["TRADERS"]
         }
    }
   """.trimIndent()

   private val trade1 = """{
      "id" : 1,
      "amount" : 300,
      "counterParty" : "desk1Client"
    }"""
   private val trade2 = """{
      "id" : 2,
      "amount" : 500,
      "counterParty" : "desk2Client"
    }"""
   private val tradeList = "[$trade1,$trade2]"

   @Test
   fun loadsPoliciesForDataType() {

      val (vyne, stubService) = testVyne(schema())
      vyne.addKeyValuePair("test.SessionToken", "aabbcc", FactSets.CALLER)
      stubService.addResponse("tokenToUserId", vyne.typedValue("test.UserId", "jimmy123"))
      stubService.addResponse("findUser", vyne.parseJsonModel("test.User", traderUser))
      stubService.addResponse("listTrades", vyne.parseJsonModel("test.Trade[]", tradeList))
      stubService.addResponse("findClient", clientHandler(vyne))

      val context = vyne.queryEngine().queryContext(queryId = "ABCD", clientQueryId = null)
      runBlocking {context.find("test.Trade[]").results.toList()}

      expect(stubService.invocations["tokenToUserId"]).to.have.size(1)
      expect(stubService.invocations["findUser"]).to.have.size(1)
      expect(stubService.invocations["findClient"]).to.have.size(1)
   }

   @Test
   fun given_policyRestrictsReturnedValue_then_nullIsReturned() {
      val (vyne, stubService) = testVyne(schema())
      vyne.addKeyValuePair("test.SessionToken", "aabbcc", FactSets.CALLER)
      val tradeResponse = vyne.parseJsonModel("test.Trade", trade1)
      stubService.addResponse("getTrade", tradeResponse)

      stubService.addResponse("tokenToUserId", vyne.typedValue("test.UserId", "jimmy123"))
      stubService.addResponse("findUser", vyne.parseJsonModel("test.User", nonTraderUser))
      stubService.addResponse("findClient", vyne.parseJsonModel("test.Client", desk1Client))
      // Trade1 is filtered because our user doesn't have a desk id.
      val context = vyne.queryEngine().queryContext(queryId = "ABCD", clientQueryId = null, additionalFacts = setOf(vyne.typedValue("TradeId", 1)))
      val queryResultsList = runBlocking {context.find("test.Trade").results.toList()}

      val trade = queryResultsList.get(0)
      expect(trade).instanceof(TypedNull::class.java)
   }

   @Test
   fun given_policyRestrictsDataReturnedInCollection_then_itIsRemovedFromTheCollection() {
      val (vyne, stubService) = testVyne(schema())
      vyne.addKeyValuePair("test.SessionToken", "aabbcc", FactSets.CALLER)
      val tradeResponse = vyne.parseJsonModel("test.Trade[]", tradeList)
      stubService.addResponse("listTrades", tradeResponse)

      stubService.addResponse("tokenToUserId", vyne.typedValue("test.UserId", "jimmy123"))
      stubService.addResponse("findUser", vyne.parseJsonModel("test.User", traderUser))

      stubService.addResponse("findClient", clientHandler(vyne))

      // Trade2 is filtered because our trader belongs to a different desl
      val context = vyne.queryEngine().queryContext(queryId = "ABCD", clientQueryId = null)
      val queryResult = runBlocking {context.find("test.Trade[]")}

      //val tradeCollection = queryResult["test.Trade[]"] as TypedCollection

      // trade 1 is present, trade 2 is filtered
      //expect(tradeCollection).to.have.size(1)
      //val trade = tradeCollection.first() as TypedObject
      //expect(trade["id"].value).to.equal(1)
   }

   private fun clientHandler(vyne: Vyne): StubResponseHandler {

      val clientHandler: StubResponseHandler = { operation: RemoteOperation, params: List<Pair<Parameter, TypedInstance>> ->
         val (_, clientId) = params.first()
         when (clientId.value) {
               "desk1Client" -> listOf(vyne.parseJsonModel("test.Client", desk1Client))
               "desk2Client" -> listOf(vyne.parseJsonModel("test.Client", desk2Client))
               else -> TODO("Unhandled client")
         }
      }
      return clientHandler
   }


   @Test
   fun given_policyPermitsData_then_itIsPresentInResponse() = runBlocking {
      val (vyne, stubService) = testVyne(schema())
      vyne.addKeyValuePair("test.SessionToken", "aabbcc", FactSets.CALLER)
      stubService.addResponse("getTrade", vyne.parseJsonModel("test.Trade", trade1))

      stubService.addResponse("tokenToUserId", vyne.typedValue("test.UserId", "jimmy123"))
      stubService.addResponse("findUser", vyne.parseJsonModel("test.User", traderUser))
      stubService.addResponse("findClient", clientHandler(vyne))

      // Trade1 is filtered because our user doesn't have a desk id.
      val context = vyne.queryEngine().queryContext(queryId = "ABCD", clientQueryId = null,additionalFacts = setOf(vyne.typedValue("TradeId", 1)))
      val queryResult = context.find("test.Trade").results
         .test {
            expectTypedInstance()
         }
   }

   @Test
   fun given_policyRestrictsNestedDataType_then_attributeIsReturnedAsNull() {
      val (vyne, stubService) = testVyne(schema())
      vyne.addKeyValuePair("test.SessionToken", "aabbcc", FactSets.CALLER)
      val tradeResponse = vyne.parseJsonModel("test.TradeWrapper", """
          {
             "wrapperText" : "Hello, world",
             "trade" : $trade1
          }
      """.trimIndent())
      stubService.addResponse("getTradeWrapper", tradeResponse)

      stubService.addResponse("tokenToUserId", vyne.typedValue("test.UserId", "jimmy123"))
      stubService.addResponse("findUser", vyne.parseJsonModel("test.User", nonTraderUser))
      stubService.addResponse("findClient", clientHandler(vyne))

      // Trade1 is filtered because our user doesn't have a desk id.
      val context = vyne.queryEngine().queryContext(queryId = "ABCD", clientQueryId = null,additionalFacts = setOf(vyne.typedValue("TradeId", 1)))
      val queryResult = runBlocking {context.find("test.TradeWrapper").results.toList()}

      val trade = queryResult.get(0)
      expect(trade).instanceof(TypedObject::class.java)
      val tradeWrapper = trade as TypedObject

      expect(tradeWrapper["wrapperText"]).instanceof(TypedValue::class.java)
      expect(tradeWrapper["trade"]).instanceof(TypedNull::class.java)
   }

   @Test
   fun given_policyFiltersAttributes_then_thoseAttributesAreReturnedAsNull() {
      val (vyne, stubService) = testVyne(schema("filter (counterParty, amount)"))
      vyne.addKeyValuePair("test.SessionToken", "aabbcc", FactSets.CALLER)
      val tradeResponse = vyne.parseJsonModel("test.Trade", trade2)
      stubService.addResponse("getTrade", tradeResponse)

      stubService.addResponse("tokenToUserId", vyne.typedValue("test.UserId", "jimmy123"))
      stubService.addResponse("findUser", vyne.parseJsonModel("test.User", traderUser))
      stubService.addResponse("findClient", clientHandler(vyne))

      // Trade2 is masked because our users deskId doesn't match
      val context = vyne.queryEngine().queryContext(queryId = "ABCD", clientQueryId = null,additionalFacts = setOf(vyne.typedValue("TradeId", 2)))
      val queryResult = runBlocking {context.find("test.Trade").results.toList()}

      val trade = queryResult.get(0) as TypedObject
      val counterParty = trade["counterParty"]
      expect(counterParty.value).to.be.`null`
   }
   // Policy processors are disbled:
   // https://gitlab.com/vyne/vyne/issues/52

//   @Test
//   fun givenPolicyMasksDataType_then_attributeIsReturnedAsMasked() {
//      val (vyne, stubService) = testVyne(schema("process using vyne.StringMasker(['counterParty'])"))
//      vyne.addKeyValuePair("test.SessionToken", "aabbcc", FactSets.CALLER)
//      val tradeResponse = vyne.parseJsonModel("test.Trade", trade2)
//      stubService.addResponse("getTrade", tradeResponse)
//
//      stubService.addResponse("tokenToUserId", vyne.typedValue("test.UserId", "jimmy123"))
//      stubService.addResponse("findUser", vyne.parseJsonModel("test.User", traderUser))
//
//      // Trade2 is masked because our users deskId doesn't match
//      val context = vyne.queryEngine().queryContext(additionalFacts = setOf(vyne.typedValue("TradeId", 2)))
//      val queryResult = context.find("test.Trade")
//
//      val trade = queryResult["test.Trade"] as TypedObject
//      val counterParty = trade["counterParty"]
//      expect(counterParty.value).to.equal(StringMaskingProcessor.MASKED_VALUE)
//   }

}

