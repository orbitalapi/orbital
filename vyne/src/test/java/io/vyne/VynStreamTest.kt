package io.vyne

import app.cash.turbine.test
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.Type
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class VynStreamTest {

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
   fun `will call all streaming enpdoints that extend a base type for a streaming query`() {
      runBlocking {
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
            operation streamKiwis():Stream<NewZealander>
            operation streamAussies():Stream<Australian>
         }
      """.trimIndent()
         )

         stub.addResponseFlow("streamKiwis") { remoteOperation, parameters ->
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
            people.asFlow().shareIn(GlobalScope, SharingStarted.Lazily)
         }

         stub.addResponseFlow("streamAussies") { remoteOperation, parameters ->
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
            people.asFlow().shareIn(GlobalScope, SharingStarted.Lazily)
         }

         vyne.query("""stream { Person }""").results.test(timeout = Duration.ZERO) {
            val events = listOf(expectTypedObject(),expectTypedObject(),expectTypedObject(),expectTypedObject())
            events.map { it["firstName"].value }.should.contain("Steve")
            events.map { it["firstName"].value }.should.contain("David")
            events.map { it["firstName"].value }.should.contain("Glenn")
            events.map { it["firstName"].value }.should.contain("Brendon")
            expectNoEvents()
         }
      }
   }
}
