package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.models.json.right
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.fail

// This is by no means the only place this is tested
// but I couldn't another clean file to write a test, and I'm a bit tired.
class ProjectionsSpec :  DescribeSpec({
   describe("queries with projections") {

      it("can return a field of a no-arg service in a projection") {
         val (vyne,stub) = testVyne("""
            closed model Person {
               id : PersonId inherits String
               name : Name inherits String
            }
            service MyApi {
               operation getPerson():Person
            }
         """.trimIndent())
         stub.addResponse("getPerson", """{ "id" : "123", "name" : "Jimmy" }""")

         // The issue here is that we're not searching for Person (which can be returned from the Direct strategy),
         // but Graph searcher can't invoke as there's no edges leading to the service.
         val result = vyne.query("""given { PersonId = "123" } find {
            | name : Name // <--- this is the test
            |}
         """.trimMargin())
            .firstRawObject()
         result.shouldBe(mapOf("name" to "Jimmy"))
      }

      // ORB-1075
      it("should allow projecting an object to a field") {
         val (vyne,stub) = testVyne("""
model SenderParty
model ReceiverParty

model PartyDetails {
   id : PartyId inherits String
   name : PartyName inherits String
}

model Party {
   id : PartyId
}

model SwiftSenderParty inherits Party, SenderParty
model SwiftReceiverParty  inherits Party, ReceiverParty

model SwiftPayment {
   id : PaymentId inherits String
   desc : PaymentDescription inherits String

   // Two objects with the same structure, but the type provides the context
   sender : SwiftSenderParty
   receiver : SwiftReceiverParty
}

service MyApi {
   operation getPayment(PaymentId):SwiftPayment
   operation getParty(PartyId):PartyDetails
}
         """.trimIndent())
         stub.addResponse("getPayment", """{
  "id" : "123",
  "desc" : "Beer",
  "sender" : { "id" : "SEND-1" },
  "receiver" : { "id" : "REC-1" }
}""")
         stub.addResponse("getParty") { _,params ->
            val id = params.first().second.value as String
            val json = when (id) {
               "SEND-1" -> """{ "id" : "SEND-1", "name" : "Jimmy" }"""
               "REC-1" -> """{ "id" : "REC-1", "name" : "Jack" }"""
               else -> fail { "Invalid id passed : $id" }
            }
            listOf(vyne.parseJson("PartyDetails", json).right())
         }

         val result = vyne.query("""
given { PaymentId = "abc" }
find { SwiftPayment } as {
    desc : PaymentDescription
    sender : SenderParty as PartyName // <--- this is the test. Was getting null here
    getter : ReceiverParty as PartyName // <--- this is the test. Was getting null here
}
         """.trimMargin())
            .firstRawObject()

         result.shouldBe(
            mapOf("desc" to "Beer", "sender" to "Jimmy", "getter" to "Jack")
         )
      }

   }
}) {
}
