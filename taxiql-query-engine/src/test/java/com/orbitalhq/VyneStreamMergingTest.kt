package com.orbitalhq

import app.cash.turbine.test
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import io.kotest.assertions.retry
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.parse
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {  }
/**
 * These are tests that explore merging two streams together
 */
class VyneStreamMergingTest : DescribeSpec({
   // These tests constantly break on the build server.
   // Commenting out until we can make them reliable ORB-195
   xdescribe("Querying joining mulitple streams") {
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

      // This test appears flaky, but app performance seems fine.
      retry(5, 60.seconds) {
         it("should run a query that joins multiple streams") {
            logger.info { "starting should run a query that joins multiple streams" }
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

            results.test(timeout = Duration.parse("5s")) {
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
      }

      // This test appears flaky, but app performance seems fine.
      retry(5, 60.seconds) {
         it("should run a query that joins multiple streams without explicit streams") {
            logger.info { "starting should run a query that joins multiple streams without explicit streams" }
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

            results.test(10.seconds) {
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
      }

      // This test appears flaky, but app performance seems fine.
      retry(5, 3.minutes) {
         it("should run a query that joins multiple streams without explicit streams and can enrich from other sources") {
            logger.info { "starting should run a query that joins multiple streams without explicit streams and can enrich from other sources" }
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

            results.test(timeout = Duration.parse("30s")) {
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
   }
})
