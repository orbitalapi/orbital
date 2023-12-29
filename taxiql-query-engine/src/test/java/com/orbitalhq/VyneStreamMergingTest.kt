package com.orbitalhq

import app.cash.turbine.test
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration

/**
 * These are tests that explore merging two streams together
 */
class VyneStreamMergingTest : DescribeSpec({
   describe("Querying joining mulitple streams") {
      val (vyne, stub) = testVyne(
         """
        model Tweet {
         @Id messageId : MessageId inherits String
         message : Message inherits String
         userId : UserId inherits Int
      }

      model TweetAnalytics {
         messageId : MessageId
         views : ViewCount inherits Int
      }

      model User {
         userName : UserName inherits String
      }

      service TweetService {
         operation tweets():Stream<Tweet>
         operation analytics():Stream<TweetAnalytics>
         operation getUser(UserId):User
      }
     """.trimIndent()
      )

      it("should run a query that joins multiple streams") {
         val tweetFlow = MutableSharedFlow<TypedInstance>()
         val analyticsFlow = MutableSharedFlow<TypedInstance>()
         stub.addResponseFlow("tweets") { _, _ -> tweetFlow }
         stub.addResponseFlow("analytics") { _, _ -> analyticsFlow }

         val results = vyne.query(
            """stream { Tweet | TweetAnalytics }
           | as {
           |   id : MessageId
           |   body : Message
           |   views : ViewCount?
           |}[]
        """.trimMargin()
         )
            .results

         results.test {
            tweetFlow.emit(vyne.parseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1}"""))
            val first = expectTypedObject()
            first.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to "Hello",
                  "views" to null
               )
            )

            analyticsFlow.emit(vyne.parseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
            val second = expectTypedObject()
            // Because we don't have anything to join the state, we should be getting nulls on values
            // that arrived on previous messages
            second.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to null,
                  "views" to 100,
               )
            )


         }
      }

      it("should run a query that joins multiple streams without explicit streams") {
         val tweetFlow = MutableSharedFlow<TypedInstance>()
         val analyticsFlow = MutableSharedFlow<TypedInstance>()
         stub.addResponseFlow("tweets") { _, _ -> tweetFlow }
         stub.addResponseFlow("analytics") { _, _ -> analyticsFlow }


         val results = vyne.query(
            """stream { Tweet } // We're only requesting a single stream
           | as {
           |   id : MessageId
           |   body : Message
           |   views : ViewCount? // Comes from TweetAnalytics
           |}[]
        """.trimMargin()
         )
            .results

         results.test {
            tweetFlow.emit(vyne.parseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1 }"""))
            val first = expectTypedObject()
            first.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to "Hello",
                  "views" to null
               )
            )

            analyticsFlow.emit(vyne.parseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
            val second = expectTypedObject()
            // Because we don't have anything to join the state, we should be getting nulls on values
            // that arrived on previous messages
            second.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to null,
                  "views" to 100
               )
            )


         }
      }

      it("should run a query that joins multiple streams without explicit streams and can enrich from other sources") {
         val tweetFlow = MutableSharedFlow<TypedInstance>()
         val analyticsFlow = MutableSharedFlow<TypedInstance>()
         stub.addResponseFlow("tweets") { _, _ -> tweetFlow }
         stub.addResponseFlow("analytics") { _, _ -> analyticsFlow }
         stub.addResponse("getUser", vyne.parseJson("User", """{ "userName" : "Jimmy" }"""))

         val results = vyne.query(
            """stream { Tweet } // We're only requesting a single stream
           | as {
           |   id : MessageId
           |   body : Message
           |   views : ViewCount? // Comes from TweetAnalytics
           |   user : UserName
           |}[]
        """.trimMargin()
         )
            .results

         results.test(timeout = Duration.parse("20s")) {
            tweetFlow.emit(vyne.parseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1}"""))
            val first = expectTypedObject()
            first.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to "Hello",
                  "views" to null,
                  "user" to "Jimmy"
               )
            )

            analyticsFlow.emit(vyne.parseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
            val second = expectTypedObject()
            // Because we don't have anything to join the state, we should be getting nulls on values
            // that arrived on previous messages
            second.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to null,
                  "views" to 100,
                  "user" to null
               )
            )


         }
      }

   }
})
