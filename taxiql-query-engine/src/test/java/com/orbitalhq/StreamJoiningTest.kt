package com.orbitalhq

import app.cash.turbine.test
import arrow.core.Either
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.models.json.tryParseJson
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.caching.MapBackedStateStoreProvider
import com.orbitalhq.query.caching.StateStoreAnnotation
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.assertions.retry
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

/**
 * These are tests that explore merging two streams together
 */
class StreamJoiningTest : DescribeSpec({
   // These tests constantly break on the build server.
   // Commenting out until we can make them reliable ORB-195
   describe("Querying joining multiple streams") {
      val defaultSchema = TaxiSchema.fromStrings(
         listOf(
            StateStoreAnnotation.StateStoreTaxi,
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
     """
         )
      )

      // This test appears flaky, but app performance seems fine.
      retry(5, 60.seconds) {
         it("a query that joins multiple streams without a state store should emit whenever either stream emits") {
            logger.info { "starting should run a query that joins multiple streams" }
            val tweetFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val analyticsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val (vyne, stub) = testVyne(defaultSchema)
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
               tweetFlow.emit(vyne.tryParseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1}"""))
               val first = expectTypedObject()
               first.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello",
                     "views" to null
                  )
               )

               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
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

         it("a query that joins multiple streams using intersection should emit after both streams have emitted") {
            logger.info { "starting should run a query that joins multiple streams" }
            val tweetFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val analyticsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val (vyne, stub) = testVyne(defaultSchema, stateStoreProvider = MapBackedStateStoreProvider())
            stub.addResponseFlow("tweets") { _, _ -> tweetFlow }
            stub.addResponseFlow("analytics") { _, _ -> analyticsFlow }

            val results = vyne.query(
               """
           | @com.orbitalhq.StateStore
           | stream { Tweet & TweetAnalytics }
           | as {
           |   id : MessageId
           |   body : Message
           |   views : ViewCount?
           |}[]
        """.trimMargin()
            )
               .results

            results.test(timeout = Duration.parse("5s")) {
               tweetFlow.emit(vyne.tryParseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1}"""))
//               expectNoEvents()
               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
               val first = expectTypedObject()
               first.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello",
                     "views" to 100,
                  )
               )
               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 150 }"""))
               val second = expectTypedObject()
               second.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello",
                     "views" to 150,
                  )
               )
            }
         }



         it("a query that joins multiple streams using intersection can be enriched in projection") {
            logger.info { "starting should run a query that joins multiple streams" }
            val tweetFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val analyticsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val (vyne, stub) = testVyne(defaultSchema, stateStoreProvider = MapBackedStateStoreProvider())
            stub.addResponseFlow("tweets") { _, _ -> tweetFlow }
            stub.addResponseFlow("analytics") { _, _ -> analyticsFlow }
            stub.addResponse("getUser", """{ "userName" : "Jimmy" }""")
            val results = vyne.query(
               """
           | @com.orbitalhq.StateStore
           | stream { Tweet & TweetAnalytics }
           | as {
           |   id : MessageId
           |   body : Message
           |   // returned from external service
           |   userName : UserName
           |   views : ViewCount
           |}[]
        """.trimMargin()
            )
               .results

            results.test(timeout = Duration.parse("5s")) {
               tweetFlow.emit(vyne.tryParseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1}"""))
//               expectNoEvents()
               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
               val first = expectTypedObject()
               first.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello",
                     "views" to 100,
                     "userName" to "Jimmy",
                  )
               )
               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 150 }"""))
               val second = expectTypedObject()
               second.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello",
                     "views" to 150,
                     "userName" to "Jimmy",
                  )
               )
            }
         }

         it("can use field shorthand in a projection of an intersection type") {
            logger.info { "starting should run a query that joins multiple streams" }
            val tweetFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val analyticsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val (vyne, stub) = testVyne(defaultSchema, stateStoreProvider = MapBackedStateStoreProvider())
            stub.addResponseFlow("tweets") { _, _ -> tweetFlow }
            stub.addResponseFlow("analytics") { _, _ -> analyticsFlow }
            stub.addResponse("getUser", """{ "userName" : "Jimmy" }""")
            val results = vyne.query(
               """
           | @com.orbitalhq.StateStore
           | stream { Tweet & TweetAnalytics }
           | as {
           |   messageId,
           |   message,
           |   // returned from external service
           |   userName : UserName
           |   views : ViewCount
           |}[]
        """.trimMargin()
            )
               .results

            results.test(timeout = Duration.parse("5s")) {
               tweetFlow.emit(vyne.tryParseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1}"""))
//               expectNoEvents()
               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
               val first = expectTypedObject()
               first.toRawObject().shouldBe(
                  mapOf(
                     "messageId" to "a",
                     "message" to "Hello",
                     "views" to 100,
                     "userName" to "Jimmy",
                  )
               )
               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 150 }"""))
               val second = expectTypedObject()
               second.toRawObject().shouldBe(
                  mapOf(
                     "messageId" to "a",
                     "message" to "Hello",
                     "views" to 150,
                     "userName" to "Jimmy",
                  )
               )
            }
         }

         // Not yet implemented - ORB-830
         xit("can filter one side of a stream") {
            val tweetFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val analyticsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val (vyne, stub) = testVyne(defaultSchema, stateStoreProvider = MapBackedStateStoreProvider())
            stub.addResponseFlow("tweets") { _, _ -> tweetFlow }
            stub.addResponseFlow("analytics") { _, _ -> analyticsFlow }

            val results = vyne.query(
               """
           | @StateStore
           | stream { Tweet | TweetAnalytics.filterEach( (ViewCount) -> ViewCount > 200 ) }
           | as {
           |   id : MessageId
           |   body : Message
           |   views : ViewCount?
           |}[]
        """.trimMargin()
            )
               .results

            results.test(timeout = Duration.parse("5s")) {
               tweetFlow.emit(vyne.tryParseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1}"""))
//               expectNoEvents()
               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
               val first = expectTypedObject()
               first.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello",
                     "views" to 100,
                  )
               )
               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 150 }"""))
               val second = expectTypedObject()
               second.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello",
                     "views" to 150,
                  )
               )
            }
         }


         it("a query that joins multiple streams using a state store should emit merged messages whenever either stream emits") {
            logger.info { "starting should run a query that joins multiple streams" }
            val tweetFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val analyticsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val (vyne, stub) = testVyne(defaultSchema, stateStoreProvider = MapBackedStateStoreProvider())
            stub.addResponseFlow("tweets") { _, _ -> tweetFlow }
            stub.addResponseFlow("analytics") { _, _ -> analyticsFlow }

            val results = vyne.query(
               """
           |@com.orbitalhq.state.StateStore
           |stream { Tweet | TweetAnalytics }
           | as {
           |   id : MessageId
           |   body : Message
           |   views : ViewCount?
           |}[]
        """.trimMargin()
            )
               .results

            results.test(timeout = Duration.parse("5s")) {
               tweetFlow.emit(vyne.tryParseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1}"""))
               val first = expectTypedObject()
               first.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello",
                     "views" to null
                  )
               )

               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
               val second = expectTypedObject()
               second.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello", // Should be populated from earlier message
                     "views" to 100,
                  )
               )
            }
         }
      }
      it("test iteration order of map") {
         val map = LinkedHashMap<String, Int>(5, 0.75f, true)
         map["a"] = 1
         map["b"] = 2
         map["c"] = 3

         // assert first state
         map.entries.map { it.key to it.value }.toList() shouldBe listOf(
            "a" to 1,
            "b" to 2,
            "c" to 3
         )

         map["b"] = 4

         // assert after modification - 'b' moves to end
         map.entries.map { it.key to it.value }.toList() shouldBe listOf(
            "a" to 1,
            "c" to 3,
            "b" to 4
         )
      }

      it("can merge and project a stream where source streams have fields of the same name with different types") {
         val (vyne, stub) = testVyne(
            TaxiSchema.fromStrings(
               listOf(
                  StateStoreAnnotation.StateStoreTaxi,
                  """
            parameter model OrderEvent {
              @Id id: OrderId inherits Int
              timestamp: OrderTimestamp inherits Instant
              customer_id: CustomerId inherits Int
            }
            parameter model ShipmentEvent {
              id: ShipmentId inherits Int
              order_id: OrderId
              timestamp: ShipmentTimestamp inherits Instant
              customer_id: CustomerId inherits Int
              warehouse: Warehouse inherits String
            }
            service StreamsService {
               stream orders : Stream<OrderEvent>
               stream shipments : Stream<ShipmentEvent>
            }
         """
               )
            ), stateStoreProvider = MapBackedStateStoreProvider()
         )
         val ordersFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
         val shipmentEventsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
         stub.addResponseFlow("orders") { _, _ -> ordersFlow }
         stub.addResponseFlow("shipments") { _, _ -> shipmentEventsFlow }

         val resultsStream = vyne.query(
            """
@com.orbitalhq.state.StateStore
stream {  OrderEvent | ShipmentEvent   } as {
    orderId : OrderId
    customerId: CustomerId
    shipmentId: ShipmentId
    warehouse: Warehouse
    order_time: OrderTimestamp
    ship_time: ShipmentTimestamp
}[]"""
         ).results
         resultsStream.test(timeout = Duration.parse("5s")) {
            ordersFlow.emit(
               vyne.tryParseJson(
                  "OrderEvent",
                  """{ "id": 503, "timestamp": "2024-11-15T15:31:10Z",  "customer_id": 8  }"""
               )
            )

            val first = expectTypedObject()
            first.toRawObject().shouldBe(
               mapOf(
                  "orderId" to 503,
                  "customerId" to 8,
                  "shipmentId" to null,
                  "warehouse" to null,
                  "order_time" to "2024-11-15T15:31:10.000Z",
                  "ship_time" to null
               )
            )

            shipmentEventsFlow.emit(
               vyne.tryParseJson(
                  "ShipmentEvent",
                  """{  "id": 1000497, "order_id": 503, "timestamp": "2024-11-15T15:31:35Z", "customer_id": 2, "warehouse": "Warehouse B" }"""
               )
            )

            val second = expectTypedObject()
            second.toRawObject().shouldBe(
               mapOf(
                  "orderId" to 503,
                  "customerId" to 2, // Intentionally testing overwriting a value here - was 8 on the previous message
                  "shipmentId" to 1000497,
                  "warehouse" to "Warehouse B",
                  "order_time" to "2024-11-15T15:31:10.000Z",
                  "ship_time" to "2024-11-15T15:31:35.000Z"
               )
            )
         }
      }

      // This test appears flaky, but app performance seems fine. This test is disabled due to unwanted side effects with query re-writing for explicit stream managements
      // see comments on QueryExpressionBuilder::build
      retry(5, 60.seconds) {
         xit("should run a query that joins multiple streams without explicit streams") {
            logger.info { "starting should run a query that joins multiple streams without explicit streams" }
            val tweetFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val analyticsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val (vyne, stub) = testVyne(defaultSchema)
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
               tweetFlow.emit(vyne.tryParseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1 }"""))
               val first = expectTypedObject()
               first.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello",
                     "views" to null
                  )
               )

               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
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

      // This test appears flaky, but app performance seems fine.This test is disabled due to unwanted side effects with query re-writing for explicit stream managements
      //      // see comments on QueryExpressionBuilder::build
      retry(5, 3.minutes) {
         xit("should run a query that joins multiple streams without explicit streams and can enrich from other sources") {
            logger.info { "starting should run a query that joins multiple streams without explicit streams and can enrich from other sources" }
            val tweetFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val analyticsFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
            val (vyne, stub) = testVyne(defaultSchema)
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
               tweetFlow.emit(vyne.tryParseJson("Tweet", """{ "messageId" : "a" , "message" : "Hello" , "userId" : 1}"""))
               val first = expectTypedObject()
               first.toRawObject().shouldBe(
                  mapOf(
                     "id" to "a",
                     "body" to "Hello",
                     "views" to null,
                     "user" to "Jimmy"
                  )
               )

               analyticsFlow.emit(vyne.tryParseJson("TweetAnalytics", """{ "messageId" : "a" , "views" : 100 }"""))
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
