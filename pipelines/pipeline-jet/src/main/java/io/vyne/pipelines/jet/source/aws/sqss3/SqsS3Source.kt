package io.vyne.pipelines.jet.source.aws.sqss3

import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.StreamSource
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.accessKey
import io.vyne.connectors.aws.core.endPointOverride
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.aws.core.secretKey
import io.vyne.models.csv.CsvFormatFactory
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.csv.CsvFormatSpecAnnotation
import io.vyne.models.format.FormatDetector
import io.vyne.pipelines.jet.api.transport.CsvRecordContentProvider
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineAwareVariableProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.http.poll.next
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import net.snowflake.client.jdbc.internal.amazonaws.services.s3.event.S3EventNotification
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.support.CronSequenceGenerator
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Level
import javax.annotation.PostConstruct
import javax.annotation.Resource

@Component
class SqsS3SourceBuilder : PipelineSourceBuilder<AwsSqsS3TransportInputSpec> {
   private val formatDetector = FormatDetector.get(listOf(CsvFormatSpec))

   companion object {
      const val NEXT_SCHEDULED_TIME_KEY = "sqss3-next-scheduled-time"
   }

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is AwsSqsS3TransportInputSpec
   }

   override fun getEmittedType(
      pipelineSpec: PipelineSpec<AwsSqsS3TransportInputSpec, *>,
      schema: Schema
   ): QualifiedName {
      return pipelineSpec.input.targetType.typeName
   }

   override fun build(
      pipelineSpec: PipelineSpec<AwsSqsS3TransportInputSpec, *>,
      inputType: Type
   ): StreamSource<MessageContentProvider> {
      val csvModelFormatAnnotation = formatDetector.getFormatType(inputType)
         ?.let { if (it.second is CsvFormatSpec) CsvFormatSpecAnnotation.from(it.first) else null }
      return SourceBuilder.timestampedStream("sqs-s3-operation-poll") { context ->
         PollingSqsOperationSourceContext(context.logger(), pipelineSpec, csvModelFormatAnnotation)
      }
         .fillBufferFn { context: PollingSqsOperationSourceContext, buffer: SourceBuilder.TimestampedSourceBuffer<MessageContentProvider> ->
            context.fill(buffer)
         }.destroyFn { context ->
            context.destroy()
         }
         .build()
   }
}


typealias SqsMessageReceiptHandle = String

@SpringAware
class PollingSqsOperationSourceContext(
   val logger: ILogger,
   val pipelineSpec: PipelineSpec<AwsSqsS3TransportInputSpec, *>,
   private val csvModelFormatAnnotation: CsvFormatSpecAnnotation?
) {


   val inputSpec: AwsSqsS3TransportInputSpec = pipelineSpec.input

   val schedule = CronSequenceGenerator(inputSpec.pollSchedule)

   var dataBuffer: LinkedBlockingQueue<Pair<MessageContentProvider, Long>> = LinkedBlockingQueue()

   private val scheduler = ThreadPoolTaskScheduler()

   init {
      scheduler.poolSize = 1
      scheduler.threadNamePrefix = "s3SqsPoller"
      scheduler.initialize()
   }

   @Resource
   lateinit var clock: Clock

   @Resource
   lateinit var connectionRegistry: AwsConnectionRegistry

   @Resource
   lateinit var variableProvider: PipelineAwareVariableProvider

   private var _lastRunTime: Instant? = null


   fun fill(buffer: SourceBuilder.TimestampedSourceBuffer<MessageContentProvider>) {
      val sink = mutableListOf<Pair<MessageContentProvider, Long>>()
      dataBuffer.drainTo(sink, 1024)
      if (sink.isNotEmpty()) {
         logger.info("Filling S3Sqs Buffer with ${sink.size} items")
         sink.forEach {
            buffer.add(it.first, it.second)
         }
      }
   }

   fun destroy() {
      try {
         scheduler.destroy()
      } catch (e: Exception) {
         logger.severe("error in closing the scheduler", e)
      }
   }

   private fun connection(): AwsConnectionConfiguration {
      return connectionRegistry.getConnection(inputSpec.connection)
   }

   private fun fetchSqsMessages(sqsClient: SqsClient): Pair<S3EventNotification, SqsMessageReceiptHandle>? {
      val messagesList = mutableListOf<Message>()
      try {
         val snsRequest = ReceiveMessageRequest.builder()
            .queueUrl(inputSpec.queueName)
            .maxNumberOfMessages(1)
            .visibilityTimeout(12 * 60 * 60) // max permissable value is 12 hours.
            .build()

         messagesList.addAll(sqsClient.receiveMessage(snsRequest).messages())
      } catch (e: Exception) {
         logger.log(Level.SEVERE, "error in retrieving from sqs queue ${inputSpec.queueName}", e)
      }

      if (messagesList.isEmpty()) {
         logger.log(Level.INFO, "There is no message in Sqs Queue!")
         return null
      }

      // I've asked for 1 message from sqs - see above maxNumberOfMessages(1)
      val firstMessage = messagesList.first()
      return try {
         val s3EventNotification = S3EventNotification.parseJson(firstMessage.body())
         if (s3EventNotification == null || s3EventNotification.records.isEmpty()) {
            null
         } else {
            Pair(s3EventNotification, firstMessage.receiptHandle())
         }
      } catch (e: Exception) {
         logger.log(Level.SEVERE, "received an unexpected event from the ${inputSpec.queueName}", e)
         deleteSqsMessage(sqsClient, firstMessage.receiptHandle())
         null
      }
   }

   private fun createSqsClient(): SqsClient {
      val connection = connection()
      val sqsClientBuilder = SqsClient
         .builder()
         .credentialsProvider(
            StaticCredentialsProvider.create(
               AwsBasicCredentials.create(
                  connection.accessKey,
                  connection.secretKey
               )
            )
         )
         .region(Region.of(connection.region))


      if (connection.endPointOverride != null) {
         sqsClientBuilder.endpointOverride(URI(connection.endPointOverride))
      }

      return sqsClientBuilder.build()
   }

   private fun closeSqsClient(sqsClient: SqsClient) {
      try {
         sqsClient.close()
      } catch (e: Exception) {
         logger.log(Level.SEVERE, "error in closing Sqs Client for ${inputSpec.queueName}", e)
      }
   }

   private fun createS3Client(): S3Client {
      val connection = connection()
      val s3Builder = S3Client
         .builder()
         .credentialsProvider(
            StaticCredentialsProvider.create(
               AwsBasicCredentials.create(
                  connection.accessKey,
                  connection.secretKey
               )
            )
         )
         .region(Region.of(connection.region))

      if (connection.endPointOverride != null) {
         s3Builder.endpointOverride(URI(connection.endPointOverride))
      }
      return s3Builder.build()
   }

   private fun closeS3Client(s3Client: S3Client) {
      try {
         s3Client.close()
      } catch (e: Exception) {
         logger.log(Level.SEVERE, "error in closing S3 Client.", e)
      }
   }

   private fun closeClientsAndScheduleNextPoll(s3Client: S3Client, sqsClient: SqsClient) {
      closeSqsClient(sqsClient)
      closeS3Client(s3Client)
      scheduleWork()
   }

   private fun deleteSqsMessage(sqsClient: SqsClient, receiptHandle: SqsMessageReceiptHandle) {
      try {
         sqsClient
            .deleteMessage(
               DeleteMessageRequest.builder()
                  .queueUrl(inputSpec.queueName)
                  .receiptHandle(receiptHandle)
                  .build()
            )
      } catch (e: Exception) {
         logger.log(
            Level.SEVERE,
            "error in deleting sqs message with receipt handle $receiptHandle fro sqs queue ${inputSpec.queueName}",
            e
         )
      }
   }


   private fun scheduleWork() {
      lastRunTime = clock.instant()
      val nextSchedule = schedule.next(lastRunTime)
      logger.info("last run time $lastRunTime next run time $nextSchedule")
      scheduler.schedule(this::doWork,nextSchedule)
   }

   private fun doWork() {
      val sqsClient = createSqsClient()
      val s3EventNotificationAndSqsReceiptHandler = fetchSqsMessages(sqsClient)
      if (s3EventNotificationAndSqsReceiptHandler == null) {
         logger.info("Sqs Poll operation returned 0 messages")
         closeSqsClient(sqsClient)
         scheduleWork()
         return
      }

      val s3Client = createS3Client()
      val s3EventNotification = s3EventNotificationAndSqsReceiptHandler.first
      s3EventNotification.records.forEach {
         val bucketName = it.s3.bucket.name
         val objectKey = it.s3.`object`.key
         logger.log(Level.INFO, "fetching $objectKey from $bucketName")
         try {
            val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(objectKey).build()
            val responseInputStream = s3Client.getObject(getObjectRequest)
            if (this.csvModelFormatAnnotation != null) {
               feedAsCsvRecord(responseInputStream, csvModelFormatAnnotation)
            } else {
               // forEach - this is not right
               val linesStream = BufferedReader(InputStreamReader(responseInputStream, StandardCharsets.UTF_8)).lines()
               linesStream.forEach { line ->
                  dataBuffer.add(Pair(StringContentProvider(line), clock.millis()))
               }
            }

         } catch (e: Exception) {
            logger.log(Level.SEVERE, "error in retrieving s3 object $objectKey from bucket $bucketName", e)
         }
      }

      deleteSqsMessage(sqsClient, s3EventNotificationAndSqsReceiptHandler.second)
      closeClientsAndScheduleNextPoll(s3Client, sqsClient)
   }

   private fun feedAsCsvRecord(
      inputStream: InputStream,
      csvModelFormatAnnotation: CsvFormatSpecAnnotation
   ) {
      val csvFormat = CsvFormatFactory.fromParameters(csvModelFormatAnnotation.ingestionParameters)
      val parser = csvFormat.parse(inputStream.bufferedReader())
      parser.forEach { csvRecord ->
         dataBuffer.add(Pair(CsvRecordContentProvider(csvRecord), clock.millis()))
      }
   }

   // Getter/Setter here to help with serialization challenges and race conditions of spring injecting the clock
   var lastRunTime: Instant
      get() {
         return _lastRunTime!!
      }
      set(value) {
         _lastRunTime = value
      }


   @PostConstruct
   fun init() {
      if (_lastRunTime == null) {
         scheduleWork()
      }
   }
}
