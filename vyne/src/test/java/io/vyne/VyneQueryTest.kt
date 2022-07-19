package io.vyne

import com.winterbe.expekt.should
import io.vyne.http.MockWebServerRule
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseJsonModel
import io.vyne.models.json.parseKeyValuePair
import io.vyne.query.VyneQlGrammar
import io.vyne.utils.withoutWhitespace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.services.QueryOperationCapability
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFails

@ExperimentalCoroutinesApi
class VyneQueryTest {

   @Rule
   @JvmField
   val server = MockWebServerRule()

   @Test
   fun canQueryAnonymousTypes(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         VyneQlGrammar.QUERY_TYPE_TAXI,
         """
         type TraderId inherits String
         type TraderName inherits String
         type TraderDeskName inherits String
         model Trade {
            traderId : TraderId
            name: TraderName
         }

         model TradingDesk {
           deskName: TraderDeskName
         }
         service TradeService {
            operation findByTraderId(TraderId): Trade
            operation findDeskByTraderId(TraderId): TradingDesk
         }
      """.trimIndent()
      )
      val response = vyne.parseJsonModel("Trade", """{ "traderId" : "jimmy", "name": "jimmy choo" }""")
      stub.addResponse("findByTraderId", response)
      stub.addResponse("findDeskByTraderId", vyne.parseJsonModel("TradingDesk","""{ "deskName" : "Inflation" }"""))
      val queryResult = vyne.query(
         """
            given { id: TraderId = 'jimmy' }
            find  {
               traderName: TraderName
               desk: TraderDeskName
             }
         """.trimIndent())
      val resultList = queryResult.rawObjects()
      resultList.should.have.size(1)
      resultList.first()["traderName"].should.equal("jimmy choo")
      resultList.first()["desk"].should.equal("Inflation")
   }


   @Test
   fun willInvokeAQueryToDiscoverValues() = runBlocking {
      val (vyne, stub) = testVyne(
         VyneQlGrammar.QUERY_TYPE_TAXI,
         """
         type TraderId inherits String
         model Trade {
            traderId : TraderId
         }
         service TradeService {
            ${queryDeclaration("tradeQuery", "Trade[]")}
         }
      """.trimIndent()
      )

      val response = vyne.parseJsonModel("Trade[]", """[ { "traderId" : "jimmy" } ]""")
      stub.addResponse("tradeQuery", response)
      val queryResult = vyne.query("findAll { Trade[]( TraderId == 'jimmy' ) }")

      val resultList = queryResult.rawObjects()
      resultList.should.have.size(1)
      resultList.first()["traderId"].should.equal("jimmy")

      val invocations = stub.invocations["tradeQuery"]!!
      invocations.should.have.size(1)
      val vyneQlQuery = invocations.first().value!! as String

      val expectedVyneQl = """findAll { lang.taxi.Array<Trade>(
          TraderId == "jimmy"
         )
      }"""
      vyneQlQuery.withoutWhitespace()
         .should.equal(expectedVyneQl.withoutWhitespace())

      println(queryResult.results?.toList())

   }

   @Test
   fun `when a value is returned containing a nested fact, that fact is used in discovery`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type PersonName inherits String
         model PersonIds {
            personId : PersonId inherits String
         }
         model Person {
            identifiers : PersonIds
         }
         service PersonService {
            operation findAllPeople():Person[]
            operation findName(PersonId):PersonName
         }
      """.trimIndent()
      )
      val people = TypedInstance.from(
         vyne.type("Person[]"),
         """[{ "identifiers" : { "personId" : "j123" } }]""",
         vyne.schema,
         source = Provided
      )
      stub.addResponse(
         "findAllPeople",
         people
      )
      stub.addResponse("findName", vyne.parseKeyValuePair("PersonName", "Jimmy"))

      val result = vyne.query(
         """findAll { Person[] } as { id : PersonId
         | name : PersonName }[]""".trimMargin()
      )
         .results.toList()
      result.first().toRawObject().should.equal(
         mapOf("id" to "j123", "name" to "Jimmy")
      )
   }

   @Test
   fun `when no path exists to discover a value then an exception is thrown`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            name : Name inherits String
         }
      """.trimIndent()
      )
      assertFails {
         val result = vyne.query("findAll { Person[] }")
            .typedObjects()
      }
   }

   @Test
   fun `when direct service exists and returns empty results then empty result set is returned`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            name : Name inherits String
         }
         service People {
            operation findPeople():Person[]
         }
      """.trimIndent()
      )
      stub.addResponse("findPeople", emptyList())
      val result = vyne.query("findAll { Person[] }")
         .typedObjects()
      result.should.be.empty
   }

   @Test
   fun `when service exists from graph search that returns empty results then empty result set is returned`(): Unit =
      runBlocking {
         val (vyne, stub) = testVyne(
            """
         model Author {
            @Id id : AuthorId inherits Int
            name : Name inherits String
         }
         model Book {
            title : BookTitle inherits String
         }

         service Library {
            operation findBooks(AuthorId):Book[]
         }
      """.trimIndent()
         )
         stub.addResponse("findBooks", emptyList())
         val result = vyne.query("given { id:AuthorId = 1 } findOne { Book[] }")
            .typedObjects()
         result.should.be.empty
      }

   @Test
   fun `when service from graph search returns collection then full set of results is returned`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Author {
            @Id id : AuthorId inherits Int
            name : Name inherits String
         }
         model Book {
            title : BookTitle inherits String
         }

         service Library {
            operation findBooks(AuthorId):Book[]
         }
      """.trimIndent()
      )
      stub.addResponse(
         "findBooks",
         vyne.parseJson("Book[]", """[ { "title" : "Harry Potter 1" } , { "title" : "Harry Potter 2" } ]""")
      )
      val result = vyne.query("given { id:AuthorId = 1 } findOne { Book[] }")
         .typedObjects()
      result.should.have.size(2)
   }

   @Test
   fun `enum comparison in when conditions`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type PersonName inherits String
         enum Sex {
            Male("male"),
            Female("female")
         }

         enum Title {
            Mr,
            Miss
         }

         model Person {
            id : PersonId as String
            sex: Sex
         }

         model Result {
            id : PersonId
            name : PersonName
            sex : Sex
            title: Title? by when {
                  this.sex == Sex.Male -> Title.Mr
                  else -> Title.Miss
               }
         }
         service PersonService {
            operation findAllPeople():Person[]
            operation findName(PersonId):PersonName
         }
      """.trimIndent()
      )
      val people = TypedInstance.from(
         vyne.type("Person[]"),
         """[{ "id"  : "j123", "sex": "male" }]""",
         vyne.schema,
         source = Provided
      )
      stub.addResponse(
         "findAllPeople",
         people
      )
      stub.addResponse("findName", vyne.parseKeyValuePair("PersonName", "Jimmy"))

      val result = vyne.query(
         """findAll { Person[] } as Result[]""".trimMargin()
      )
         .results.toList()
      result.first().toRawObject().should.equal(
         mapOf("id" to "j123", "name" to "Jimmy", "sex" to "male", "title" to "Mr")
      )
   }

   @Test
   fun `failures in boolean expression evalution should not terminate when condition evalutaions`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type PersonName inherits String
         type Theme inherits String
         enum Sex {
            Male("male"),
            Female("female")
         }

         enum Title {
            Mr,
            Miss,
            Unknown
         }

         model Person {
            id : PersonId as String
            sex: Sex
         }

         model Result {
            id : PersonId
            name : PersonName
            sex : Sex
            title: Title? by when {
                  this.sex == Sex.Male && Theme == null -> Title.Unknown
                  this.sex == Sex.Male -> Title.Mr
                  else -> Title.Miss
               }
         }
         service PersonService {
            operation findAllPeople():Person[]
            operation findName(PersonId):PersonName
         }
      """.trimIndent()
      )
      val people = TypedInstance.from(
         vyne.type("Person[]"),
         """[{ "id"  : "j123", "sex": "male" }]""",
         vyne.schema,
         source = Provided
      )
      stub.addResponse(
         "findAllPeople",
         people
      )
      stub.addResponse("findName", vyne.parseKeyValuePair("PersonName", "Jimmy"))

      val result = vyne.query(
         """findAll { Person[] } as Result[]""".trimMargin()
      )
         .results.toList()
      result.first().toRawObject().should.equal(
         mapOf("id" to "j123", "name" to "Jimmy", "sex" to "male", "title" to "Mr")
      )
   }


}

fun queryDeclaration(
   queryName: String,
   returnTypeName: String,
   capabilities: List<QueryOperationCapability> = QueryOperationCapability.ALL
): String {
   return """
      vyneQl query $queryName(params:${VyneQlGrammar.QUERY_TYPE_NAME}):$returnTypeName with capabilities {
         ${capabilities.joinToString(", \n") { it.asTaxi() }}
      }
   """.trimIndent()
}
