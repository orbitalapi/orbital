package com.orbitalhq

import com.winterbe.expekt.should
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.UnresolvedTypeInQueryException
import com.orbitalhq.schemas.fqn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.fail

@ExperimentalCoroutinesApi
class VyneServiceInvocationTest {

   @Test
   fun `do not call invalid services`() = runBlockingTest {
      val (vyne, stub) = testVyne(
         """
         model Trade {
            @Id
            tradeId : TradeId as String
            traderId : TraderUserId as String
            isin : Isin as String
            settlementDate : SettlementDate as Date
         }
         model Output {
            @FirstNotEmpty
            settlementDate : SettlementDate as Date
         }
         model Product {
            @Id
            isin : Isin
            settlementDate : SettlementDate
         }

         @DataStore
         service TradeService {
            @StubResponse("findAllTrades")
            operation findAllTrades():Trade[]

            // We don't want this to be called.
            @StubResponse("findTradesBySalesPerson")
            operation findTradesBySalesPerson(TraderUserId):Trade
            // We don't want this to be called.
            @StubResponse("findTrade")
            operation findTrade(TradeId):Trade
         }
         service ProductService {
         @StubResponse("findProductData")
            operation findProductData(Isin):Product
         }
      """.trimIndent()
      )
      val tradeJson = """{
         |"tradeId" : "trade1",
         |"traderId" : "jimmy",
         |"isin" : "tradeIsin",
         |"settlementDate" : null
         |}
      """.trimMargin()
      val trade = TypedInstance.from(vyne.type("Trade"), tradeJson, vyne.schema, source = Provided)

      stub.addResponse("findAllTrades", TypedCollection.from(listOf(trade)))
      vyne.query("""find { Trade[] } as Output[]""")

   }


   @Test
   fun `service is not invoked if cannot satisfy contract`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            @Id id : PersonId inherits String
            firstName : FirstName inherits String
         }
         service PersonService {
            operation listPeople():Person[]
         }
      """.trimIndent()
      )

      stub.addResponse("listPeople", TypedCollection.empty(vyne.type("Person[]")))
      try {
         val results = vyne.query("""find { Person[]( PersonId == '1' ) }""").rawObjects()
      } catch (e: UnresolvedTypeInQueryException) {
         stub.invocations.should.be.empty
         e.typeName.should.equal("Person[]".fqn())
         return@runBlocking
      }

      fail("Expected an exception thrown")
   }

   @Test
   fun `facts provided in a given will be used in a service invocation`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            personId: PersonId inherits String
            name : PersonName inherits String
         }
         type AuthKey inherits String
         type AuthSecret inherits String
         service Peeps {
            operation findPerson(AuthKey,AuthSecret) : Person
         }
      """.trimIndent()
      )
      stub.addResponse("findPerson", vyne.parseJson("Person", """{ "personId" : 1, "name" : "Jimmy"}"""))
      val result = vyne.query("""given { AuthKey = '123', AuthSecret = '234' } find { Person } """)
         .rawObjects()
      result.single().should.equal(mapOf("personId" to "1", "name" to "Jimmy"))
      val args = stub.invocations["findPerson"]!!
      args.should.have.size(2)
      args[0].typeName.should.equal("AuthKey")
      args[0].value.should.equal("123")
      args[1].typeName.should.equal("AuthSecret")
      args[1].value.should.equal("234")
   }

   @Test
   fun `facts provided in a given can be used to populate a parameter object`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            personId: PersonId inherits String
            name : PersonName inherits String
         }
         type AuthKey inherits String
         type AuthSecret inherits String

         parameter model RequestObject {
            authKey : AuthKey
            secret : AuthSecret
         }
         service Peeps {
            operation findPerson(RequestObject) : Person
         }
      """.trimIndent()
      )
      stub.addResponse("findPerson", vyne.parseJson("Person", """{ "personId" : 1, "name" : "Jimmy"}"""))
      val result = vyne.query("""given { AuthKey = '123', AuthSecret = '234' } find { Person } """)
         .rawObjects()
      result.single().should.equal(mapOf("personId" to "1", "name" to "Jimmy"))
      val args = stub.invocations["findPerson"]!!
      args.should.have.size(1)
      args[0].type.longDisplayName.should.equal("RequestObject")
      args[0].toRawObject().should.equal(
         mapOf(
            "authKey" to "123",
            "secret" to "234"
         )
      )
   }

   @Test
   fun `facts provided in a given can be used to populate a parameter object when invoking a service in a projection`(): Unit =
      runBlocking {
         val (vyne, stub) = testVyne(
            """
         model Person {
            personId: PersonId inherits String
         }
         model PersonDetails {
            name : PersonName inherits String
         }
         type AuthKey inherits String
         type AuthSecret inherits String

         parameter model RequestObject {
            personId : PersonId
            authKey : AuthKey
            secret : AuthSecret
         }
         service Peeps {
            operation findPerson() : Person
            operation findPersonDetails(RequestObject):PersonDetails
         }
      """.trimIndent()
         )
         stub.addResponse("findPerson", vyne.parseJson("Person", """{ "personId" : 1 }"""))
         stub.addResponse("findPersonDetails", vyne.parseJson("PersonDetails", """{ "name" : "Jimmy"  }"""))
         val result = vyne.query(
            """given { AuthKey = '123', AuthSecret = '234' }
         |find { Person } as {
         |  id : PersonId
         |  name : PersonName
         |}
      """.trimMargin()
         )
            .rawObjects()
         result.single().should.equal(mapOf("id" to "1", "name" to "Jimmy"))
         val args = stub.invocations["findPersonDetails"]!!
         args.should.have.size(1)
         args[0].type.longDisplayName.should.equal("RequestObject")
         args[0].toRawObject().should.equal(
            mapOf(
               "personId" to "1",
               "authKey" to "123",
               "secret" to "234"
            )
         )
      }


}