package com.orbitalhq

import app.cash.turbine.test
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf

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
      }

      model TweetAnalytics {
         messageId : MessageId
         views : ViewCount inherits Int
      }

      service TweetService {
         operation tweets():Stream<Tweet>
         operation analytics():Stream<TweetAnalytics>
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
            tweetFlow.emit(vyne.parseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" }"""))
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
})
