package io.vyne.connectors.aws.dynamodb

import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.vyne.StubService
import io.vyne.connectors.aws.core.AwsConnection
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.VyneQlGrammar
import io.vyne.rawObjects
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.testVyne
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*

@Testcontainers
class DynamoDbInvokerTest {

    private val localStackImage = DockerImageName.parse("localstack/localstack").withTag("1.0.4")

    @Container
    var localStack: LocalStackContainer = LocalStackContainer(localStackImage)
        .withServices(LocalStackContainer.Service.DYNAMODB)

    private val connectionRegistry = AwsInMemoryConnectionRegistry()

    @BeforeEach
    fun beforeTest() {
        val connectionConfig = AwsConnectionConfiguration(
            connectionName = "vyneAws",
            mapOf(
                AwsConnection.Parameters.ACCESS_KEY.templateParamName to localStack.accessKey,
                AwsConnection.Parameters.SECRET_KEY.templateParamName to localStack.secretKey,
                AwsConnection.Parameters.AWS_REGION.templateParamName to localStack.region,
                AwsConnection.Parameters.ENDPOINT_OVERRIDE.templateParamName to localStack.getEndpointOverride(
                    LocalStackContainer.Service.DYNAMODB
                ).toString()
            )
        )
        connectionRegistry.register(connectionConfig)
    }

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
                    1 -> listOf(TypedInstance.from(schema.type("Movie"), starWars, schema, source = Provided))
                    2 -> listOf(TypedInstance.from(schema.type("Movie"), solarisMovieJson, schema, source = Provided))
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

         model Movie {
            @Id
            id : MovieId
            title : MovieTitle
         }

         @io.vyne.aws.dynamo.Table( connectionName = "vyneAws" , tableName = "reviews" )
         model Review {
            @Id
            movieId : MovieId
            score: ReviewScore inherits Int
         }

         service MovieService {
            operation listMovies(): Movie[]
            operation findMovie(MovieId): Movie
         }

         @io.vyne.aws.dynamo.DynamoService( connectionName = "vyneAws" )
         service DynamoService {
            table reviews: Review[]
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

    private fun String.asAttribute(): AttributeValue = AttributeValue.fromS(this)
    private fun Int.asAttribute() = AttributeValue.fromN(this.toString())

    private fun buildReviewsTable(data: List<Map<String, AttributeValue>>) {
        val client = buildClient()

        val tableName = "reviews"
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

    private fun buildClient(): DynamoDbClient = DynamoDbClient.builder()
        .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
        .region(Region.of(localStack.region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    localStack.accessKey,
                    localStack.secretKey
                )
            )
        )
        .build()

}
