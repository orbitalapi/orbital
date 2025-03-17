package com.orbitalhq

import app.cash.turbine.test
import arrow.core.Either
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.right
import com.orbitalhq.models.json.tryParseJson
import com.orbitalhq.query.StreamErrorMessage
import com.winterbe.expekt.should
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import reactor.kotlin.test.test
import kotlin.time.Duration

class StreamsWithErrorsTest : DescribeSpec({
  describe("errors in streams") {
      val testDuration = "1000s"
      val testSchema = """
          closed model UserUpdateMessage {
            userId : UserId inherits String
            message : StatusMessage inherits String
         }
         
         model UserDepartment {
            userId: UserId
            department: Department inherits String
         }
       
         service UserService {
            operation getUpdates():Stream<UserUpdateMessage>
            operation getUserDepartment(userId: UserId): UserDepartment
         }
      """.trimIndent()
     it("should report an error and continue if a parsing error happens on source data") {
        val (vyne, stub) = testVyne(testSchema)
         val userUpdateFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 2)
        stub.addResponseFlow("getUpdates") { _, _ -> userUpdateFlow }

        //First Message in the system is corrupted.
        //Second Message is valid.
        val queryResult = vyne.query(
           """
         stream { UserUpdateMessage }
      """.trimIndent()
        )

         userUpdateFlow.tryEmit(vyne.tryParseJson("UserUpdateMessage", """{ "userId" : "aaa", "message" : "bla }"""))
         userUpdateFlow.tryEmit(vyne.tryParseJson("UserUpdateMessage", """{ "userId" : "bbb", "message" : "Stretching" }"""))


         queryResult.results.test(timeout = Duration.parse(testDuration)) {
             expectTypedObject()
                 .toRawObject()
                 .should.equal(mapOf("userId" to "bbb", "message" to "Stretching"))

         }

        queryResult.errors.test()
             .expectSubscription()
             .expectNextMatches {
                 it.error.typeName == "UserUpdateMessage" &&
                         it.error.message == "Unexpected end-of-input: was expecting closing quote for a string value\n" +
                         " at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 39]"
             }
             .thenCancel()
             .verify()


     }
     it("should report an error and continue if a parsing error happens on enrichment data") {
         val (vyne, stub) = testVyne(testSchema)
         val schema = vyne.schema
         val userUpdateFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 2)
         stub.addResponseFlow("getUpdates") { _, _ -> userUpdateFlow }
         stub.addResponse("getUserDepartment") {  _, inputs ->
             when (val userId = inputs.first().second.value.toString()) {
                 "aaa" -> listOf(TypedInstance.tryFrom(schema.type("UserDepartment"), """{ "userId": "$userId", "department": "IT }""", schema, source = Provided))
                 // invalid json for department.
                 "bbb" -> listOf(TypedInstance.tryFrom(schema.type("UserDepartment"), """{ "userId": "$userId", "department": "IT" }""", schema, source = Provided))
                 else -> error("Invalid person id")
             }
         }

         //First Message in the system is corrupted.
         //Second Message is valid.
         val queryResult = vyne.query(
             """
         stream { UserUpdateMessage } as { 
          userId: UserId
          department: Department
          }[]
      """.trimIndent()
         )

         userUpdateFlow.tryEmit(vyne.tryParseJson("UserUpdateMessage", """{ "userId" : "aaa", "message" : "bla" }"""))
         userUpdateFlow.tryEmit(vyne.tryParseJson("UserUpdateMessage", """{ "userId" : "bbb", "message" : "Stretching" }"""))


         queryResult.results.test(timeout = Duration.parse(testDuration)) {
             // `aaa` has a malformed json coming from the enrichment service and hence will have a null department.
             expectTypedObject()
                 .toRawObject()
                 .should.equal(mapOf("userId" to "aaa", "department" to null))

            // `bbb` department is coming fine from the enrichment service
             expectTypedObject()
                 .toRawObject()
                 .should.equal(mapOf("userId" to "bbb", "department" to "IT"))
         }

         queryResult.errors.test()
             .expectSubscription()
             .expectNextMatches {
                 it.error.typeName == "UserDepartment" &&
                         it.error.message == "Unexpected end-of-input: was expecting closing quote for a string value\n" +
                         " at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 39]"
             }
             .thenCancel()
             .verify()

     }
     it("should report an error and continue if a enrichment service throws an error") {
         val (vyne, stub) = testVyne(testSchema)
         val schema = vyne.schema
         val userUpdateFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 2)
         stub.addResponseFlow("getUpdates") { _, _ -> userUpdateFlow }
         stub.addResponse("getUserDepartment") {  _, inputs ->
             when (val userId = inputs.first().second.value.toString()) {
                 "bbb" -> listOf(TypedInstance.tryFrom(schema.type("UserDepartment"), """{ "userId": "$userId", "department": "IT" }""", schema, source = Provided))
                 // enrichment service throws an error
                 else -> error("Invalid person id")
             }
         }

         //First Message in the system is corrupted.
         //Second Message is valid.
         val queryResult = vyne.query(
             """
         stream { UserUpdateMessage } as { 
          userId: UserId
          department: Department
          }[]
      """.trimIndent()
         )

         userUpdateFlow.tryEmit(vyne.tryParseJson("UserUpdateMessage", """{ "userId" : "aaa", "message" : "bla" }"""))
         userUpdateFlow.tryEmit(vyne.tryParseJson("UserUpdateMessage", """{ "userId" : "bbb", "message" : "Stretching" }"""))


         queryResult.results.test(timeout = Duration.parse(testDuration)) {
             // Enrichment service throws for `aaa`
             expectTypedObject()
                 .toRawObject()
                 .should.equal(mapOf("userId" to "aaa", "department" to null))

             // `bbb` department is coming fine from the enrichment service
             expectTypedObject()
                 .toRawObject()
                 .should.equal(mapOf("userId" to "bbb", "department" to "IT"))
         }

         queryResult.errors.test()
             .expectSubscription()
             .expectNextMatches {
                 it.error.typeName == "UserDepartment" &&
                         it.error.message == "Invalid person id"
             }
             .thenCancel()
             .verify()


     }
     it("should report an error and continue if we can't call an enrichment service on a specific message because there's insufficient data to populate param message") {
         // This scenario requires further refinement as insufficient data for an enrichment call yields UnresolvedTypeInQueryException
         // within the flow and we use that to explore alternative search paths in the code.
     }

     it("should report an error and continue if writing mutation fails") {
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
             ).map { vyne.tryParseJson("UserUpdateMessage", it) }
                 .asFlow()
         }
         stub.addResponse("getUser") { _, params ->
             val userId = params[0].second.value!!
             val username = when (userId) {
                 "aaa" -> "Jimmy"
                 "bbb" -> "Mike"
                 else -> error("Unexpected user id")
             }
             val user = vyne.tryParseJson("User", """{ "id" : "$userId", "name" : "$username" } """)
             listOf(user)
         }
         stub.addResponse("storeUpdate") { _, params ->
             params.map {
                 val model = it.second as TypedObject
                 when(model["userId"].value) {
                     "aaa" -> model.right()
                     else -> Either.Left(StreamErrorMessage.fromException(IllegalStateException("invalid user Id"), "RichUserUpdateMessage"))
                 }
             }
         }


        val results = vyne.query(
             """
         stream { UserUpdateMessage }
         call UserService::storeUpdate
      """.trimIndent()
         )
         val queryResult =results.rawObjects()
         queryResult.shouldHaveSize(1)

         results.errors.test()
             .expectSubscription()
             .expectNextMatches {
                 it.error.typeName == "RichUserUpdateMessage" &&
                         it.error.message == "invalid user Id"
             }
             .thenCancel()
             .verify()
     }
     it("should report an error and continue if parsing the response from a mutation fails") {
         // MAJORITY OF OUR INVOKERS DON'T USE THE RESPONSE FROM THE MUTATION!!
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
             ).map { vyne.tryParseJson("UserUpdateMessage", it) }
                 .asFlow()
         }
         stub.addResponse("getUser") { _, params ->
             val userId = params[0].second.value!!
             val username = when (userId) {
                 "aaa" -> "Jimmy"
                 "bbb" -> "Mike"
                 else -> error("Unexpected user id")
             }
             val user = vyne.tryParseJson("User", """{ "id" : "$userId", "name" : "$username" } """)
             listOf(user)
         }
         stub.addResponse("storeUpdate") { _, params ->
             params.map {
                 val model = it.second as TypedObject
                 val userId = model["userId"].value
                 when(userId) {
                     "aaa" -> model.right()
                     else -> vyne.tryParseJson("RichUserUpdateMessage", """{ "userId" : "aaa", "name" : "bla }""")
                 }
             }
         }


         val results = vyne.query(
             """
         stream { UserUpdateMessage }
         call UserService::storeUpdate
      """.trimIndent()
         )
         val queryResult =results.rawObjects()
         queryResult.shouldHaveSize(1)

         results.errors.test()
             .expectSubscription()
             .expectNextMatches {
                 it.error.typeName == "RichUserUpdateMessage" &&
                         it.error.message == "Unexpected end-of-input: was expecting closing quote for a string value\n" +
                         " at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 36]"
             }
             .thenCancel()
             .verify()

     }
  }
})
