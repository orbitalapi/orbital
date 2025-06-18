package com.orbitalhq.connectors.aws.dynamodb

import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.query.tracing.DatabaseResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyne
import com.orbitalhq.typedInstances
import com.orbitalhq.typedObjects
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

@Testcontainers
class DynamoDbInvokerTest : BaseDynamoTest() {

   @Test
   fun `can load from dynamo and enrich to another source`(): Unit = runBlocking {
      val vyne = testVyne(schemaSrc)
      { schema ->
         val stubService = StubService(schema = schema)
         val starWars = """{
         |"id" : "1",
         |"title" : "Star Wars"
         |}
      """.trimMargin()
         val solarisMovieJson = """{
         |"id" : "2",
         |"title" : "Empire Strikes Back"
         |}
      """.trimMargin()
         stubService.addResponse("findMovie") { _, inputs ->
            val movieId = inputs.first().second.value.toString().toInt()
            when (movieId) {
               1 -> listOf(TypedInstance.tryFrom(schema.type("Movie"), starWars, schema, source = Provided))
               2 -> listOf(TypedInstance.tryFrom(schema.type("Movie"), solarisMovieJson, schema, source = Provided))
               else -> error("Invalid movie id")
            }
         }
         listOf(DynamoDbInvoker(connectionRegistry, SimpleSchemaProvider(schema)), stubService)
      }

      buildReviewsTable(
         data = listOf(
            mapOf("movieId" to 1.asAttribute(), "score" to 3.asAttribute()),
            mapOf("movieId" to 2.asAttribute(), "score" to 4.asAttribute())
         )
      )
      val result = vyne.query(
         """
            find { Review[] } as {
                id : MovieId
                title : MovieTitle
                reviewScore: ReviewScore
            }[]
        """.trimIndent()
      )
         .rawObjects()

      result.shouldContainAll(
         mapOf("id" to 1, "title" to "Star Wars", "reviewScore" to 3),
         mapOf("id" to 2, "title" to "Empire Strikes Back", "reviewScore" to 4),
      )
      result.shouldHaveSize(2)
   }

   val schemaSrc = listOf(
      DynamoConnectorTaxi.schema,
      VyneQlGrammar.QUERY_TYPE_TAXI,
      """

        import ${VyneQlGrammar.QUERY_TYPE_NAME}
        type MovieId inherits Int
         type MovieTitle inherits String
         type StreamingProviderName inherits String
         type MonthlyPrice inherits Decimal
         type NetflixMovieId inherits String

         model Movie {
            @Id
            id : MovieId
            title : MovieTitle
         }

         @com.orbitalhq.aws.dynamo.Table( connectionName = "vyneAws" , tableName = "reviews" )
         model Review {
            @Id
            movieId : MovieId
            score: ReviewScore inherits Int
         }

         service MovieService {
            operation listMovies(): Movie[]
            operation findMovie(MovieId): Movie
         }

         @com.orbitalhq.aws.dynamo.DynamoService
         service DynamoService {
            table reviews: Review[]

            @UpsertOperation
            write operation saveReview(Review):Review
         }
        """
   )


   @Test
   fun `can load from another source and enrich from dynamodb`(): Unit = runBlocking {
      val vyne = testVyne(schemaSrc)
      { schema ->
         val stubService = StubService(schema = schema)
         val stalkerMovieJson = """{
         |"id" : "1",
         |"title" : "Stalker"
         |}
      """.trimMargin()
         val solarisMovieJson = """{
         |"id" : "2",
         |"title" : "Solaris"
         |}
      """.trimMargin()
         val stalker = TypedInstance.from(schema.type("Movie"), stalkerMovieJson, schema, source = Provided)
         val solaris = TypedInstance.from(schema.type("Movie"), solarisMovieJson, schema, source = Provided)
         stubService.addResponse("listMovies", TypedCollection.from(listOf(stalker, solaris)))
         listOf(DynamoDbInvoker(connectionRegistry, SimpleSchemaProvider(schema)), stubService)
      }

      buildReviewsTable(
         data = listOf(
            mapOf("movieId" to 1.asAttribute(), "score" to 3.asAttribute()),
            mapOf("movieId" to 2.asAttribute(), "score" to 4.asAttribute())
         )
      )
      val result = vyne.query(
         """
            find { Movie[] } as {
                id : MovieId
                reviewScore: ReviewScore
            }[]
        """.trimIndent()
      )
         .rawObjects()

      result.shouldBe(
         listOf(
            mapOf("id" to 1, "reviewScore" to 3),
            mapOf("id" to 2, "reviewScore" to 4),
         )
      )
      result.shouldHaveSize(2)
   }

   @Test
   fun `when lookup in dynamo doesnt match then typed null is returned`(): Unit = runBlocking {
      val vyne = testVyne(schemaSrc)
      { schema ->
         val stubService = StubService(schema = schema)
         val stalkerMovieJson = """{
         |"id" : "1",
         |"title" : "Stalker"
         |}
      """.trimMargin()
         // THIS IS THE TEST.
         // MOVIE 3 DOESN'T EXIST IN DYNAMO
         val solarisMovieJson = """{
         |"id" : "3",
         |"title" : "Solaris"
         |}
      """.trimMargin()
         val stalker = TypedInstance.from(schema.type("Movie"), stalkerMovieJson, schema, source = Provided)
         val solaris = TypedInstance.from(schema.type("Movie"), solarisMovieJson, schema, source = Provided)
         stubService.addResponse("listMovies", TypedCollection.from(listOf(stalker, solaris)))
         listOf(DynamoDbInvoker(connectionRegistry, SimpleSchemaProvider(schema)), stubService)
      }

      // MOVIE WITH ID 3 DOESN'T EXIST IN DYANAMO
      buildReviewsTable(
         data = listOf(
            mapOf("movieId" to 1.asAttribute(), "score" to 3.asAttribute()),
         )
      )
      val result = vyne.query(
         """
            find { Movie[] } as {
                id : MovieId
                reviewScore: ReviewScore
            }[]
        """.trimIndent()
      )
         .rawObjects()

      result.shouldBe(
         listOf(
            mapOf("id" to 1, "reviewScore" to 3),
            mapOf("id" to 3, "reviewScore" to null),
         )
      )
      result.shouldHaveSize(2)
   }


   @Test
   fun `can expose a mutation and write a new record`(): Unit = runBlocking {
      val vyne = testVyne(schemaSrc)
      { schema ->
         listOf(DynamoDbInvoker(connectionRegistry, SimpleSchemaProvider(schema)))
      }
      buildReviewsTable(
         data = emptyList()
      )
      val saved = vyne.query(
         """
            given { r: Review = {  movieId : 200 , score: 5 } }
            call DynamoService::saveReview
        """.trimIndent()
      )
         .rawObjects()

      // Now try and find it again
      val loaded = vyne.query("""find { Review( MovieId == 200 ) } """)
         .rawObjects()
      loaded.shouldHaveSize(1)
      loaded.single().shouldBe(
         mapOf("movieId" to 200, "score" to 5)
      )
   }


   private fun buildReviewsTable(data: List<Map<String, AttributeValue>>) {
      val client = buildClient()

      val tableName = "reviews"

      // Delete if it already exists
      try {
         client.deleteTable(
            DeleteTableRequest
            .builder().tableName(tableName)
            .build()
         )
      } catch (e: ResourceNotFoundException) {}

      client.createTable { tableBuilder ->
         tableBuilder.tableName(tableName)
            .keySchema(KeySchemaElement.builder().attributeName("movieId").keyType(KeyType.HASH).build())
            .attributeDefinitions(
               AttributeDefinition.builder().attributeName("movieId").attributeType(ScalarAttributeType.N).build(),
//                    AttributeDefinition.builder().attributeName("score").attributeType(ScalarAttributeType.N).build()
            )
            .provisionedThroughput(
               ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build()
            )
      }
      client.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(tableName).build())
      data.forEach { recordValues ->
         client.putItem(PutItemRequest.builder().tableName(tableName).item(recordValues).build())
      }
   }

   @Test
   fun `emits tracing events when querying and includes actual response data in payload`(): Unit = runBlocking {
      val vyne = testVyne(schemaSrc) { schema ->
         listOf(DynamoDbInvoker(connectionRegistry, SimpleSchemaProvider(schema)))
      }

      buildReviewsTable(
         data = listOf(
            mapOf("movieId" to 1.asAttribute(), "score" to 3.asAttribute()),
            mapOf("movieId" to 2.asAttribute(), "score" to 4.asAttribute())
         )
      )

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """find { Review[]( MovieId == 1 ) }""",
         eventBroker = eventBroker
      ).rawObjects()

      // Should have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify request event
      val requestEvent = eventSink.collectedEvents.first()
      requestEvent.spanState.shouldBe(SpanState.ACTIVE)
      val requestMetadata = requestEvent.exchangeMetadata.shouldBeInstanceOf<DatabaseRequest>()
      requestEvent.eventVerb.shouldBe("Query")
      val requestPayload = requestMetadata.payload()
      requestPayload.shouldNotBeNull()
      requestPayload.shouldContain("movieId")

      // Verify response event contains actual DynamoDB response data
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<DatabaseResponse>()
      responseMetadata.recordCount.shouldBe(1)

      // Verify payload contains actual response data (JSON serialized DynamoDB items)
      val responsePayload = responseMetadata.payload()
      responsePayload.shouldNotBeNull()
      responsePayload.shouldContain("movieId")
      responsePayload.shouldContain("score")
      responsePayload.shouldContain("N=3") // DynamoDB AttributeValue format

      // Verify the actual query result
      result.shouldHaveSize(1)
      result.first().shouldBe(mapOf("movieId" to 1, "score" to 3))
   }

   @Test
   fun `emits tracing events when doing GetItem operations`(): Unit = runBlocking {
      val vyne = testVyne(schemaSrc) { schema ->
         listOf(DynamoDbInvoker(connectionRegistry, SimpleSchemaProvider(schema)))
      }

      buildReviewsTable(
         data = listOf(
            mapOf("movieId" to 1.asAttribute(), "score" to 5.asAttribute())
         )
      )

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """find { Review( MovieId == 1 ) }""",
         eventBroker = eventBroker
      ).rawObjects()

      // Should have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify response event for GetItem
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      responseEvent.eventVerb.shouldBe("GetItem response")
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<DatabaseResponse>()
      responseMetadata.recordCount.shouldBe(1)

      // Verify payload contains the actual item data
      val responsePayload = responseMetadata.payload()
      responsePayload.shouldNotBeNull()
      responsePayload.shouldContain("movieId")
      responsePayload.shouldContain("score")
      responsePayload.shouldContain("N=5") // DynamoDB AttributeValue format

      result.shouldBe(listOf(mapOf("movieId" to 1, "score" to 5)))
   }

   @Test
   fun `emits tracing events when no items found`(): Unit = runBlocking {
      val vyne = testVyne(schemaSrc) { schema ->
         listOf(DynamoDbInvoker(connectionRegistry, SimpleSchemaProvider(schema)))
      }

      buildReviewsTable(data = emptyList())

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """find { Review( MovieId == 999 ) }""",
         eventBroker = eventBroker
      ).typedInstances()

      // Should still have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify response event for no items found
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<DatabaseResponse>()
      responseMetadata.recordCount.shouldBe(0)

      // Verify payload indicates no item found
      val responsePayload = responseMetadata.payload()
      responsePayload.shouldNotBeNull()
   }

   @Test
   fun `emits tracing events when doing upsert operations and includes actual response data`(): Unit = runBlocking {
      val vyne = testVyne(schemaSrc) { schema ->
         listOf(DynamoDbInvoker(connectionRegistry, SimpleSchemaProvider(schema)))
      }

      buildReviewsTable(data = emptyList())

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """
         given { r: Review = { movieId : 200, score: 5 } }
         call DynamoService::saveReview
         """.trimIndent(),
         eventBroker = eventBroker
      ).rawObjects()

      // Should have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify request event
      val requestEvent = eventSink.collectedEvents.first()
      requestEvent.spanState.shouldBe(SpanState.ACTIVE)
      val requestMetadata = requestEvent.exchangeMetadata.shouldBeInstanceOf<DatabaseRequest>()
      requestEvent.eventVerb.shouldBe("Upsert")
      val requestPayload = requestMetadata.payload()
      requestPayload.shouldNotBeNull()
      requestPayload.shouldContain("PutItemRequest")

      // Verify response event contains actual DynamoDB response data
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      responseEvent.eventVerb.shouldBe("Upsert response")
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<DatabaseResponse>()
      responseMetadata.recordCount.shouldBe(1)

      // Verify payload contains actual response data (serialized PutItemResponse)
      val responsePayload = responseMetadata.payload()
      responsePayload.shouldNotBeNull()
      responsePayload.shouldContain("PutItemResponse") // Should contain the actual AWS response
   }

}
