package com.orbitalhq.connectors.aws.sqs

import com.orbitalhq.ErrorType
import com.orbitalhq.Vyne
import com.orbitalhq.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.firstRawObject
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.format.DefaultFormatRegistry
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import com.winterbe.expekt.should
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

private val logger = KotlinLogging.logger { }

@Testcontainers
class SqsInvokerTest {
   private val sqsQueueName = "movies"
   private val localStackImage = DockerImageName.parse("localstack/localstack").withTag("3.0")

   @JvmField
   @Rule
   var localstack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.SQS)
   var sqsQueueUrl = ""

   private val connectionRegistry = AwsInMemoryConnectionRegistry()

   private fun defaultSchema(sqsQueue: String) = """
               ${SqsConnectorTaxi.Annotations.imports}
               type MovieId inherits String
               type MovieTitle inherits String
               type ActorId inherits String
               type ActorName inherits String
               
               model Movie {
                  id : MovieId
                  title : MovieTitle
               }
               
               model Actor {
                   id: ActorId
                   name: ActorName
              
               }

               @SqsService( connectionName = "moviesConnection" )
               service MovieService {
                  @SqsOperation( queue = "$sqsQueue" )
                  operation streamMovieQuery():Stream<Movie>

                  @SqsOperation( queue = "$sqsQueue" )
                  write operation publishMovie(Movie):Movie
                  
                   @SqsOperation( queue = "invalidQueueName" )
                  operation streamActorQuery():Stream<Actor>
               }

            """.trimIndent()

   @Before
   fun before() {
      sqsQueueUrl = createSqsQueue()
      val endPointOverride = localstack.getEndpointOverride(
         LocalStackContainer.Service.SQS
      ).toASCIIString()
      val connectionConfig = AwsConnectionConfiguration(
         connectionName = "moviesConnection",
          region = localstack.region,
          accessKey = localstack.accessKey,
          secretKey = localstack.secretKey,
          endPointOverride = endPointOverride
      )
      connectionRegistry.register(connectionConfig)
   }

   @Test
   fun `can write to sqs`(): Unit = runBlocking {
      val (vyne, _) = vyneWithSqsInvoker()

      val publishedMovie = vyne.query("""
         given { movie : Movie = { id: "1223" , title : "Star Wars" } }
         call MovieService::publishMovie
      """.trimIndent()).firstRawObject()

      val result = vyne.query("""stream { Movie }""")
         .results.take(1).toList() as List<TypedObject>

      result.shouldHaveSize(1)

   }


   @Test
   fun `can consume from  sqs`(): Unit = runBlocking {
      val (vyne, _) = vyneWithSqsInvoker()

      val message1 = """{"id": "1234","title": "Title 1"}"""
      val message2 = """{"id": "5678","title": "Title 2"}"""

      populateSqs(message1)
      populateSqs(message2)

      val result = vyne.query("""stream { Movie }""")
         .results.take(2).toList() as List<TypedObject>

      result.should.have.size(2)
   }


   @Test
   fun `can consume filtered stream from  sqs`(): Unit = runBlocking {
      val (vyne, _) = vyneWithSqsInvoker()

      val message1 = """{"id": "1234","title": "Star Wars"}"""
      val message2 = """{"id": "5678","title": "Jaws"}"""

      populateSqs(message1)
      populateSqs(message2)

      val result = vyne.query("""stream { Movie.filterEach( (MovieTitle) -> MovieTitle == "Jaws" ) }""")
         .results.take(1).toList() as List<TypedObject>

      result.should.have.size(1)
   }

   @Test
   fun `sqs listener tries 3 times and then exists when the sqs name is invalid`(): Unit = runBlocking {
      val (vyne, _) = vyneWithSqsInvoker()
      try {
         vyne.query("""stream { Actor }""").results.take(1).toList()
      } catch (e: Exception) {
         e.message.should.equal("Retries exhausted: 3/3 in a row (3 total)")
      }


   }

   private fun vyneWithSqsInvoker(taxi: String = defaultSchema(sqsQueueUrl)): Pair<Vyne, SqsStreamManager> {
      val schema = TaxiSchema.fromStrings(
         listOf(
            SqsConnectorTaxi.schema,
            ErrorType.queryErrorVersionedSource.content,
            taxi
         )
      )
      val schemaProvider = SimpleSchemaProvider(schema)
      val connectionBuilder = SqsConnectionBuilder(connectionRegistry, DefaultFormatRegistry.empty())
      val sqsStreamManager = SqsStreamManager(connectionBuilder, schemaProvider)
      val invokers = listOf(
         SqsInvoker(schemaProvider, sqsStreamManager, connectionBuilder)
      )
      return testVyne(schema, invokers) to sqsStreamManager
   }

   private fun createSqsQueue(): String {
      val sqsClient = SqsClient
         .builder()
         .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
         .region(Region.of(localstack.region))
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("not-used", "not-used")))
         .build()

      val sqsQueue = sqsClient.createQueue(CreateQueueRequest.builder().queueName(sqsQueueName).build()).queueUrl()
      sqsClient.close()
      return sqsQueue
   }

   private fun populateSqs(messageBody: String) {
      val sqsClient = SqsClient
         .builder()
         .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
         .region(Region.of(localstack.region))
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("not-used", "not-used")))
         .build()

      logger.info { "publising $messageBody to $sqsQueueUrl" }
      val sqsMessage = SendMessageRequest.builder().messageBody(messageBody).queueUrl(sqsQueueUrl).build()
      sqsClient.sendMessage(sqsMessage)
      sqsClient.close()
   }
}
