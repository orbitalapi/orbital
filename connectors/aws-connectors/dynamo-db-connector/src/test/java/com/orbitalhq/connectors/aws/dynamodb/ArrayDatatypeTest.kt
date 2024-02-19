package com.orbitalhq.connectors.aws.dynamodb

import com.orbitalhq.Vyne
import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedObject
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
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
import java.math.BigDecimal

class ArrayDatatypeTest : BaseDynamoTest() {
   private fun buildTestVyne(partialSchema: String): Vyne {
      return testVyne(
         listOf(
            DynamoConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         @com.orbitalhq.aws.dynamo.DynamoService
         service DynamoService {
            table movies: Movie[]

            @UpsertOperation
            write operation saveMovie(Movie):Movie
         }

         """.trimIndent(),
            partialSchema
         )
      ) { schema ->
         listOf(DynamoDbInvoker(connectionRegistry, SimpleSchemaProvider(schema)))
      }
   }

   @Test
   fun `can read string set attribute`(): Unit = runBlocking {
      val vyne = buildTestVyne(
         """
         type ActorName inherits String
         @com.orbitalhq.aws.dynamo.Table( connectionName = "vyneAws" , tableName = "movies" )
         model Movie {
            @Id
            id : FilmId inherits String
            cast : ActorName[]
         }
      """.trimIndent()
      )

      buildMoviesTable(
         listOf(
            mapOf(
               "id" to "starwars".asAttribute(),
               "cast" to listOf("Mark", "Carrie", "Harrison").asAttribute(),
            )
         )
      )

      val result = vyne.query(
         """
         find { Movie[] }
      """.trimIndent()
      ).firstTypedObject()
      val cast = result["cast"] as TypedCollection
      cast.type.name.shortDisplayName.shouldBe("ActorName[]")
      cast.first().type.name.shortDisplayName.shouldBe("ActorName")
      val actualActorNames =
         (cast.toRawObject() as List<String>).toSet() // cast to set, as dynamodb uses set, and doesn't guarantee order
      val expectedActorName = setOf("Mark", "Carrie", "Harrison")
      actualActorNames.shouldBe(expectedActorName)
   }

   @Test
   fun `can read number set attribute`(): Unit = runBlocking {
      val vyne = buildTestVyne(
         """
         type ReviewScore inherits Decimal
         @com.orbitalhq.aws.dynamo.Table( connectionName = "vyneAws" , tableName = "movies" )
         model Movie {
            @Id
            id : FilmId inherits String
            reviewScores : ReviewScore[]
         }
      """.trimIndent()
      )

      buildMoviesTable(
         listOf(
            mapOf(
               "id" to "starwars".asAttribute(),
               "reviewScores" to listOf(4.4.toBigDecimal(), 4.2.toBigDecimal(), 5.0.toBigDecimal()).asNsAttribute()
            )
         )
      )

      val result = vyne.query(
         """
         find { Movie[] }
      """.trimIndent()
      ).firstTypedObject()
      val reviews = result["reviewScores"] as TypedCollection
      reviews.type.name.shortDisplayName.shouldBe("ReviewScore[]")
      reviews.first().type.name.shortDisplayName.shouldBe("ReviewScore")
      val actualReviewScores =
         (reviews.toRawObject() as List<BigDecimal>).toSet() // cast to set, as dynamodb uses set, and doesn't guarantee order
      actualReviewScores.shouldBe(setOf(4.4.toBigDecimal(), 4.2.toBigDecimal(), 5.toBigDecimal()))
   }

   @Test
   fun `can write stringSet`(): Unit = runBlocking {
      val vyne = buildTestVyne(
         """
         type ActorName inherits String
         @com.orbitalhq.aws.dynamo.Table( connectionName = "vyneAws" , tableName = "movies" )
         model Movie {
            @Id
            id : FilmId inherits String
            cast : ActorName[]
         }
      """.trimIndent()
      )
      buildMoviesTable(emptyList())
      val writeResult = vyne.query(
         """
         given { movie : Movie = { id : "starwars", cast :  ["Mark", "Carrie", "Harrison"] } }
         call DynamoService::saveMovie
      """.trimIndent()
      ).rawObjects()
      writeResult.shouldHaveSize(1)
      val readResult = vyne.query(
         """
         find { Movie[] }
      """.trimIndent()
      ).firstRawObject()
      val actorNames = (readResult["cast"] as List<String>).toSet()
      actorNames.shouldBe(setOf("Mark", "Carrie", "Harrison"))
   }

   @Test
   fun `can write number set`(): Unit = runBlocking {
      val vyne = buildTestVyne(
         """
         type ReviewScore inherits Decimal
         @com.orbitalhq.aws.dynamo.Table( connectionName = "vyneAws" , tableName = "movies" )
         model Movie {
            @Id
            id : FilmId inherits String
            reviewScores : ReviewScore[]
         }
      """.trimIndent()
      )
      buildMoviesTable(emptyList())
      val writeResult = vyne.query(
         """
         given { movie : Movie = { id : "starwars", reviewScores :  [4.4, 4.2, 4.8] } }
         call DynamoService::saveMovie
      """.trimIndent()
      ).rawObjects()
      writeResult.shouldHaveSize(1)
      val readResult = vyne.query(
         """
         find { Movie[] }
      """.trimIndent()
      ).firstRawObject()
      val reviewScores = (readResult["reviewScores"] as List<String>).toSet()
      reviewScores.shouldBe(setOf(4.4.toBigDecimal(), 4.2.toBigDecimal(), 4.8.toBigDecimal()))
   }


   @Test
   fun `can write nested object list`(): Unit = runBlocking {
      val vyne = buildTestVyne(
         """
         model Actor {
            name : PersonName inherits String
         }
         @com.orbitalhq.aws.dynamo.Table( connectionName = "vyneAws" , tableName = "movies" )
         model Movie {
            @Id
            id : FilmId inherits String
            cast : Actor[]
         }
      """.trimIndent()
      )
      buildMoviesTable(emptyList())
      val writeResult = vyne.query(
         """
         given { movie : Movie = { id : "starwars", cast :  [{ name : "Mark" }, { name: "Carrie" }] } }
         call DynamoService::saveMovie
      """.trimIndent()
      ).rawObjects()
      writeResult.shouldHaveSize(1)
      val readResult = vyne.query(
         """
         find { Movie[] }
      """.trimIndent()
      ).firstRawObject()
      val cast = (readResult["cast"] as List<Map<String, Any>>).toSet()
      cast.shouldBe(
         setOf(
            mapOf("name" to "Mark"), mapOf("name" to "Carrie"),
         )
      )
   }

   @Test
   fun `can write nested object`(): Unit = runBlocking {
      val vyne = buildTestVyne(
         """
         model Actor {
            name : PersonName inherits String
         }
         @com.orbitalhq.aws.dynamo.Table( connectionName = "vyneAws" , tableName = "movies" )
         model Movie {
            @Id
            id : FilmId inherits String
            starring : Actor
         }
      """.trimIndent()
      )
      buildMoviesTable(emptyList())
      val writeResult = vyne.query(
         """
         given { movie : Movie = { id : "starwars", starring :  { name : "Mark" }  } }
         call DynamoService::saveMovie
      """.trimIndent()
      ).rawObjects()
      writeResult.shouldHaveSize(1)
      val readResult = vyne.query(
         """
         find { Movie[] }
      """.trimIndent()
      ).firstRawObject()
      val cast = readResult["starring"] as Map<String, Any>
      cast.shouldBe(
            mapOf("name" to "Mark")
      )
   }

   private fun buildMoviesTable(data: List<Map<String, AttributeValue>>) {
      val client = buildClient()

      val tableName = "movies"
      // Delete if it already exists
      try {
         client.deleteTable(DeleteTableRequest
            .builder().tableName(tableName)
            .build()
         )
      } catch (e:ResourceNotFoundException) {}
      client.createTable { tableBuilder ->
         tableBuilder.tableName(tableName)
            .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
            .attributeDefinitions(
               AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
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

}

