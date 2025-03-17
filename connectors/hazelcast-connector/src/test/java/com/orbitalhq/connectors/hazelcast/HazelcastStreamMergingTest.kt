package com.orbitalhq.connectors.hazelcast

import app.cash.turbine.test
import arrow.core.Either
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.orbitalhq.Vyne
import com.orbitalhq.expectTypedObject
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.tryParseJson
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.caching.StateStore
import com.orbitalhq.query.caching.StateStoreAnnotation
import com.orbitalhq.query.caching.StateStoreConfig
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyne
import com.orbitalhq.utils.Ids
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import lang.taxi.types.SumType
import mu.KotlinLogging
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

class HazelcastStreamMergingTest : DescribeSpec({
   isolationMode = IsolationMode.InstancePerTest
   describe("Joining multiple streams using Hazelcast") {
      fun buildVyneAndHazelcast(mapName: String = Ids.id("Hazelcast-")): Triple<Vyne, StubService, HazelcastInstance> {
         val hazelcast = TestHazelcastInstanceFactory(1).newHazelcastInstance()

         val schema = TaxiSchema.fromStrings(
            listOf(
            StateStoreAnnotation.StateStoreTaxi,
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
         ))

         val stateStoreProvider: StateStoreProvider = object : StateStoreProvider {
            override fun getStateStore(
               stateStoreConfig: StateStoreConfig,
               sumType: SumType,
               schema: Schema,
               emitMode: StateStoreProvider.EmitMode,
               namePrefix: String
            ): StateStore? {
               stateStoreConfig.connection.shouldBe("localHzc")
               return HazelcastStateStore(hazelcast.getMap(mapName), schema, sumType, emitMode)
            }

         }
         val (vyne, stub) = testVyne(
            schema,
            stateStoreProvider = stateStoreProvider
         )
         return Triple(vyne, stub, hazelcast)
      }

      it("should run a query that joins multiple streams") {

         val (vyne, stub, hz) = buildVyneAndHazelcast()
         val tweetFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 1)
         val analyticsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 1)
         stub.addResponseFlow("tweets") { _, _ -> tweetFlow }
         stub.addResponseFlow("analytics") { _, _ -> analyticsFlow }
         logger.info { "running a query with StateStore" }
         val results = vyne.query(
            """@com.orbitalhq.state.StateStore(connection = "localHzc")
           | stream { Tweet | TweetAnalytics }
           | as {
           |   id : MessageId
           |   body : Message
           |   views : ViewCount?
           |}[]
        """.trimMargin()
         )
            .results

         results.test(timeout = 30.seconds) {
            logger.info { "emitting a tweet" }
            tweetFlow.emit(vyne.tryParseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" }"""))
            val first = expectTypedObject()
            first.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to "Hello",
                  "views" to null
               )
            )

            analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "tweetId" : "a" , "views" : 100 }"""))
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
         logger.info { "shutting down hazelcast" }
         hz.shutdown()
      }

      it("streams should not be joined when statestore is not specified") {
         val (vyne, stub, hz) = buildVyneAndHazelcast()
         val tweetFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 1)
         val analyticsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(replay = 1)
         stub.addResponseFlow("tweets") { _, _ -> tweetFlow }
         stub.addResponseFlow("analytics") { _, _ -> analyticsFlow }

         logger.info { "running a query without StateStore" }
         val resultsWithoutStateStore = vyne.query(
            """
           | stream { Tweet | TweetAnalytics }
           | as {
           |   id : MessageId
           |   body : Message
           |   views : ViewCount?
           |}[]
        """.trimMargin()
         )
            .results

         resultsWithoutStateStore.test(timeout = 30.seconds) {
            tweetFlow.emit(vyne.tryParseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" }"""))
            val first = expectTypedObject()
            first.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to "Hello",
                  "views" to null
               )
            )

            analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "tweetId" : "a" , "views" : 100 }"""))
            val second = expectTypedObject()

            // StateStore is not defined in the query, so merging should not happen.
            second.toRawObject().shouldBe(
               mapOf(
                  "id" to "a",
                  "body" to null,
                  "views" to 100
               )
            )

         }
         logger.info { "shutting down hazelcast" }
         hz.shutdown()
      }
   }
})
