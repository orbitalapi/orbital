package com.orbitalhq.connectors.hazelcast

import com.orbitalhq.expectTypedObject
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import app.cash.turbine.test
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.StubService
import com.orbitalhq.Vyne
import com.orbitalhq.query.caching.StateStore
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.Ids
import kotlin.time.Duration.Companion.seconds

class HazelcastStreamMergingTest : DescribeSpec({
   describe("Joining multiple streams using Hazelcast") {
      fun buildVyneAndHazelcast(mapName: String = Ids.id("Hazelcast-")):Pair<Vyne,StubService> {
         val hazelcast = TestHazelcastInstanceFactory(1).newHazelcastInstance()

         val schema = TaxiSchema.from(
            """
        model Tweet {
         @Id messageId : MessageId inherits String
         message : Message inherits String
      }

      model TweetAnalytics {
         @Id
         tweetId : MessageId
         views : ViewCount inherits Int
      }

      service TweetService {
         operation tweets():Stream<Tweet>
         operation analytics():Stream<TweetAnalytics>
      }
         """.trimIndent()
         )

         val stateStoreProvider: StateStoreProvider = object : StateStoreProvider {
            override fun getCacheStore(connectionName: String, key: String, schema: Schema): StateStore? {
                return HazelcastStateStore(hazelcast, mapName, schema)
            }
         }
         val (vyne, stub) = testVyne(
            schema,
            stateStoreProvider = stateStoreProvider
         )
         return vyne to stub
      }


      it("should run a query that joins multiple streams") {
         val (vyne,stub) = buildVyneAndHazelcast()
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

         results.test(timeout = 30.seconds) {
            tweetFlow.emit(vyne.parseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" }"""))
            val first = expectTypedObject()
            first.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to "Hello",
                  "views" to null
               )
            )

            analyticsFlow.emit(vyne.parseJson("TweetAnalytics", """{ "tweetId" : "a" , "views" : 100 }"""))
            val second = expectTypedObject()

            // This is the real test.
            // We should be getting the value of the first tweet (body = hello)
            // merged with the value of the second message (views = 100)
            second.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to "Hello",
                  "views" to 100
               )
            )


         }
      }
   }
})
