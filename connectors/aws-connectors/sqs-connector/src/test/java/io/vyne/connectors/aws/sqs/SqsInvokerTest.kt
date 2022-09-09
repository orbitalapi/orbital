package io.vyne.connectors.aws.sqs

import com.winterbe.expekt.should
import io.vyne.Vyne
import io.vyne.connectors.aws.core.AwsConnection
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import io.vyne.models.TypedObject
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
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
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

private val logger = KotlinLogging.logger { }

@Testcontainers
class SqsInvokerTest {
   private val sqsQueueName = "movies"
   private val localStackImage = DockerImageName.parse("localstack/localstack").withTag("1.0.4")

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

               model Movie {
                  id : MovieId
                  title : MovieTitle
               }

               @SqsService( connectionName = "moviesConnection" )
               service MovieService {
                  @SqsOperation( queue = "$sqsQueue" )
                  operation streamMovieQuery():Stream<Movie>
               }

            """.trimIndent()

   @Before
   fun before() {
      sqsQueueUrl = createSqsQueue()
      val connectionConfig = AwsConnectionConfiguration(
         connectionName = "moviesConnection",
         mapOf(
            AwsConnection.Parameters.ACCESS_KEY.templateParamName to localstack.accessKey,
            AwsConnection.Parameters.SECRET_KEY.templateParamName to localstack.secretKey,
            AwsConnection.Parameters.AWS_REGION.templateParamName to localstack.region,
            AwsConnection.Parameters.ENDPOINT_OVERRIDE.templateParamName to localstack.getEndpointOverride(
               LocalStackContainer.Service.S3
            ).toString()
         )
      )
      connectionRegistry.register(connectionConfig)
   }


   @Test
   fun `can consume from  sqs`(): Unit = runBlocking {
      val (vyne, _) = vyneWithSqsInvoker()

      val message1 = "{\"id\": \"1234\",\"title\": \"Title 1\"}"
      val message2 = "{\"id\": \"5678\",\"title\": \"Title 2\"}"

      populateSqs(message1)
      populateSqs(message2)

      val result = vyne.query("""stream { Movie }""")
         .results.take(2).toList() as List<TypedObject>

      result.should.have.size(2)

   }


   private fun vyneWithSqsInvoker(taxi: String = defaultSchema(sqsQueueUrl)): Pair<Vyne, SqsStreamManager> {
      val schema = TaxiSchema.fromStrings(
         listOf(
            SqsConnectorTaxi.schema,
            taxi
         )
      )
      val schemaProvider = SimpleSchemaProvider(schema)
      val sqsStreamManager = SqsStreamManager(connectionRegistry, schemaProvider)
      val invokers = listOf(
         SqsInvoker(schemaProvider, sqsStreamManager)
      )
      return testVyne(schema, invokers) to sqsStreamManager
   }

   private fun createSqsQueue(): String {
      val sqsClient = SqsClient
         .builder()
         .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
         .region(Region.of(localstack.region))
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
         .build()

      logger.info { "publising $messageBody to $sqsQueueUrl" }
      val sqsMessage = SendMessageRequest.builder().messageBody(messageBody).queueUrl(sqsQueueUrl).build()
      sqsClient.sendMessage(sqsMessage)
      sqsClient.close()
   }
}
