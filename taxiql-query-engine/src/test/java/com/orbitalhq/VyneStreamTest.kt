package com.orbitalhq

import app.cash.turbine.test
import app.cash.turbine.testIn
import com.winterbe.expekt.should
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VyneStreamTest {
   @Test
   fun `will enrich a stream against a rest api`() = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type FilmId inherits Int
         model Film {
            @Id
            filmId : FilmId
            title : FilmTitle inherits String
         }
         model NewReleaseAnnouncement {
            filmId : FilmId
         }
         service FilmService {
            operation lookupFilm(FilmId):Film
            operation streamAnnouncements():Stream<NewReleaseAnnouncement>
         }
      """.trimIndent()
      )
      stub.addResponse("lookupFilm", vyne.parseJson("Film", """{ "filmId" : 1, "title" : "A new hope" }"""))
      stub.addResponseFlow("streamAnnouncements") { _, _ ->
         val typedInstance = TypedInstance.from(
            vyne.type("NewReleaseAnnouncement"),
            mapOf("filmId" to 1),
            vyne.schema
         )
         listOf(typedInstance).asFlow()
      }

      vyne.query(
         """stream { NewReleaseAnnouncement } as {
         | filmId : FilmId
         | title : FilmTitle
         | }[]
      """.trimMargin()
      ).results.test {
         val item1 = expectTypedObject()
         item1.toRawObject().should.equal(
            mapOf(
               "filmId" to 1,
               "title" to "A new hope"
            )
         )
         awaitComplete()

      }
   }

   @Test
   fun `will call streaming enpdoint for a streaming query`() {

      runBlocking {
         val (vyne, stub) = testVyne(
            """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         service PersonService {
            operation streamPeople():Stream<Person>
         }
      """.trimIndent()
         )
         stub.addResponseFlow("streamPeople") { remoteOperation, parameters ->
            val people = listOf(
               mapOf(
                  "firstName" to "Jimmy",
                  "lastName" to "Schmitt"
               ),
               mapOf(
                  "firstName" to "Jack",
                  "lastName" to "Spratt"
               )
            ).map { TypedInstance.from(vyne.type("Person"), it, vyne.schema, source = Provided) }
            people.asFlow().shareIn(GlobalScope, SharingStarted.Lazily)
         }

         vyne.query("""stream { Person }""").results.test {

            val item1 = expectTypedObject()
            val item2 = expectTypedObject()
            item1["firstName"].value.should.equal("Jimmy")
            item1["lastName"].value.should.equal("Schmitt")

            item2["firstName"].value.should.equal("Jack")
            item2["lastName"].value.should.equal("Spratt")

            expectNoEvents()
         }
      }
   }


   @Test
   fun `will call all streaming endpoints that extend a base type for a streaming query`() {
      runTest {
         val (vyne, stub) = testVyne(
            """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         // Just a little bit better....
         model NewZealander inherits Person {
            isAwesome : IsAwesome inherits Boolean
         }
         model Australian inherits Person {
            isntAwesome : IsntAwesome inherits Boolean
         }
         service PersonService {
            operation streamKiwis(): Stream<NewZealander>
            operation streamAussies(): Stream<Australian>
         }
      """.trimIndent()
         )

         stub.addResponseFlow("streamKiwis") { _, _ ->
            val people = listOf(
               mapOf(
                  "firstName" to "Glenn",
                  "lastName" to "Turner"
               ),
               mapOf(
                  "firstName" to "Brendon",
                  "lastName" to "McCullum"
               )
            )
               .map { TypedInstance.from(vyne.type("Person"), it, vyne.schema, source = Provided) }
            people.asFlow().shareIn(this, SharingStarted.Lazily)
         }

         stub.addResponseFlow("streamAussies") { _, parameters ->
            val people = listOf(
               mapOf(
                  "firstName" to "Steve",
                  "lastName" to "Smith"
               ),
               mapOf(
                  "firstName" to "David",
                  "lastName" to "Warner"
               )
            )
               .map { TypedInstance.from(vyne.type("Person"), it, vyne.schema, source = Provided) }
            people.asFlow().shareIn(this, SharingStarted.Lazily)
         }

         val turbine = vyne.query("stream { Person }").results.testIn(this)
         val events = listOf(
            turbine.expectTypedObject(),
            turbine.expectTypedObject(),
            turbine.expectTypedObject(),
            turbine.expectTypedObject()
         ).map { it["firstName"].value }
         events.should.contain("Steve")
         events.should.contain("David")
         events.should.contain("Glenn")
         events.should.contain("Brendon")
         turbine.expectNoEvents()
         turbine.cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun `can call mutation for each member of a stream`():Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """

         model UserUpdateMessage {
            userId : UserId inherits String
            message : StatusMessage inherits String
         }
         model User {
            id : UserId
            name : UserName inherits String
         }
         parameter model RichUserUpdateMessage {
            userId : UserId
            name : UserName
            message : StatusMessage
         }

         service UserService {
            operation getUser(UserId):User
            operation getUpdates():Stream<UserUpdateMessage>
            write operation storeUpdate(RichUserUpdateMessage):RichUserUpdateMessage
         }
      """.trimIndent()

      )

      stub.addResponseFlow("getUpdates") { _, _ ->
         listOf(
            """{ "userId" : "aaa", "message" : "Fighting a dragon" }""",
            """{ "userId" : "bbb", "message" : "Stretching" }"""
         ).map { vyne.parseJson("UserUpdateMessage", it) }
            .asFlow()
      }
      stub.addResponse("getUser") {_,params ->
         val userId = params[0].second.value!!
         val username = when (userId) {
            "aaa" -> "Jimmy"
            "bbb" -> "Mike"
            else -> error("Unexpected user id")
         }
         val user = vyne.parseJson("User", """{ "id" : "$userId", "name" : "$username" } """)
         listOf(user)
      }
      stub.addResponse("storeUpdate") { _, params -> params.map { it.second }
      }


      val queryResult = vyne.query("""
         stream { UserUpdateMessage }
         call UserService::storeUpdate
      """.trimIndent()).rawObjects()
      queryResult.shouldHaveSize(2)

   }
}
