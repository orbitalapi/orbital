package com.orbitalhq.query.runtime.core.dispatcher.aws.sqs

import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.awaitility.Awaitility
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import com.orbitalhq.auth.schemes.EmptyAuthSchemeRepository
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.http.ServicesConfigRepository
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryFailedException
import com.orbitalhq.query.QueryResponseMessage
import com.orbitalhq.query.runtime.QueryMessage
import com.orbitalhq.query.runtime.QueryMessageCborWrapper
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.Ids
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.kotlin.test.test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import java.util.concurrent.TimeUnit

@Testcontainers
@Ignore("Currently failing.  No-one is using this at present, (using lambdas with http endpoints instead) so will park this")
class AwsSqsQueryDispatcherTest {


   private val localStackImage: DockerImageName = DockerImageName.parse("localstack/localstack").withTag("1.0.4")

   @JvmField
   @Rule
   var localstack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.SQS)

   @Rule
   @JvmField
   val configRoot = TemporaryFolder()

   val objectMapper = Jackson.newObjectMapperWithDefaults()

   lateinit var sqsClient: SqsAsyncClient
   lateinit var queryQueueUrl: String

   @Before
   fun setUp() {
      sqsClient = SqsAsyncClient
         .builder()
         .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
         .region(Region.of(localstack.region))
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("not-used", "not-used")))
         .build()

      queryQueueUrl =
         sqsClient.createQueue(CreateQueueRequest.builder().queueName(AwsSqsQueryDispatcher.QUERY_QUEUE_NAME).build())
            .get().queueUrl()
   }

   @Test
   fun sendsQueryToQueue() {
      val dispatcher = createDispatcher()
      val payload = mapOf("message" to "Hello, world")
      var replyQueueUrl: String? = null
      dispatcher.dispatchQuery("find { Foo }", clientQueryId = Ids.id("query"))
         .test()
         .expectSubscription()
         .then {
            // read the query message from the queue
            val queryMessage = consumeQueryMessage()
            sendResultToResultQueue(queryMessage.replyTo!!, payload)
            replyQueueUrl = queryMessage.replyTo
         }
         .expectNext(payload)
         .verifyComplete()

      verifyQueueDeleted(replyQueueUrl!!)
   }

   @Test
   fun returnsErrorWhenQueryFails() {
      val dispatcher = createDispatcher()
      var replyQueueUrl: String? = null
      dispatcher.dispatchQuery("find { Foo }", clientQueryId = Ids.id("query"))
         .test()
         .expectSubscription()
         .then {
            // read the query message from the queue
            val queryMessage = consumeQueryMessage()
            sendErrorResultToQueue(queryMessage.replyTo!!, "The query failed")
            replyQueueUrl = queryMessage.replyTo
         }
         .expectErrorMatches { error ->
            error is QueryFailedException
            error.message!!.shouldBe("The query failed")
            true
         }
         .verify()

      verifyQueueDeleted(replyQueueUrl!!)
   }

   @Test
   fun returnsStreamingResponses() {
      val dispatcher = createDispatcher()
      fun messagePayload(message: String) = mapOf("message" to message)
      var replyQueueUrl: String? = null
      dispatcher.dispatchQuery("find { Foo }", clientQueryId = Ids.id("query"))
         .test()
         .expectSubscription()
         .then {
            // read the query message from the queue
            val queryMessage = consumeQueryMessage()
            sendStreamResultToResultQueue(queryMessage.replyTo!!, messagePayload("message1"))
            replyQueueUrl = queryMessage.replyTo
         }
         .expectNext(messagePayload("message1"))
         .then { sendStreamResultToResultQueue(replyQueueUrl!!, messagePayload("message2")) }
         .expectNext(messagePayload("message2"))
         .then { sendStreamCompletedMessage(replyQueueUrl!!) }
         .verifyComplete()

      verifyQueueDeleted(replyQueueUrl!!)
   }

   private fun verifyQueueDeleted(replyQueueUrl: String) {
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until<Boolean> {
         val queues = sqsClient.listQueues().get()
         !queues.queueUrls().contains(replyQueueUrl)
      }
   }

   private fun sendErrorResultToQueue(replyTo: String, error: String): SendMessageResponse {
      val message = QueryResponseMessage(
         null,
         QueryResponseMessage.QueryResponseMessageKind.ERROR,
         error
      )
      return writeMessageToResponseQueue(replyTo, message)
   }

   private fun sendStreamResultToResultQueue(replyTo: String, payload: Any): SendMessageResponse {
      return writeMessageToResponseQueue(
         replyTo, QueryResponseMessage(
            payload,
            QueryResponseMessage.QueryResponseMessageKind.STREAM_MESSAGE,
         )
      )
   }
   private fun sendStreamCompletedMessage(replyTo: String):SendMessageResponse {
      return writeMessageToResponseQueue(replyTo, QueryResponseMessage(null, QueryResponseMessage.QueryResponseMessageKind.END_OF_STREAM))
   }

   private fun sendResultToResultQueue(replyTo: String, payload: Any): SendMessageResponse {
      val message = QueryResponseMessage(
         payload,
         QueryResponseMessage.QueryResponseMessageKind.RESULT
      )
      return writeMessageToResponseQueue(replyTo, message)
   }

   private fun writeMessageToResponseQueue(
      replyTo: String,
      message: QueryResponseMessage
   ): SendMessageResponse = sqsClient.sendMessage(
      SendMessageRequest.builder()
         .queueUrl(replyTo)
         .messageBody(
            objectMapper.writeValueAsString(
               message
            )
         )
         .build()
   ).get()

   private fun consumeQueryMessage(): QueryMessage {
      val queryMessageResponse = sqsClient.receiveMessage(
         ReceiveMessageRequest.builder()
            .queueUrl(queryQueueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(10)
            .build()
      ).get()
      queryMessageResponse.hasMessages().shouldBeTrue()
      val messageBody = queryMessageResponse.messages().single().body()
      val wrapper = objectMapper.readValue<QueryMessageCborWrapper>(messageBody)
      return wrapper.message()
   }

   private fun createDispatcher(): AwsSqsQueryDispatcher {
      val schemaProvider = SimpleSchemaProvider(TaxiSchema.empty())
      return AwsSqsQueryDispatcher(
         ServicesConfigRepository(configRoot!!.root.resolve("services.conf").toPath()),
         EmptyAuthSchemeRepository,
          SourceLoaderConnectorsRegistry(configRoot!!.root.resolve("connections.conf").toPath()),
         schemaProvider,
         sqsClient,
         queryQueueAddress = queryQueueUrl
      )
   }
}
