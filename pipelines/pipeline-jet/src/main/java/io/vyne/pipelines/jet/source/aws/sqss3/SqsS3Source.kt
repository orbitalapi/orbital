package io.vyne.pipelines.jet.source.aws.sqss3

import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.configureWithExplicitValuesIfProvided
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.models.csv.CsvFormatFactory
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.csv.CsvFormatSpecAnnotation
import io.vyne.models.format.FormatDetector
import io.vyne.pipelines.jet.api.transport.CsvRecordContentProvider
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.MessageSourceWithGroupId
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.PipelineSourceType
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import net.snowflake.client.jdbc.internal.amazonaws.services.s3.event.S3EventNotification
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Level
import javax.annotation.PostConstruct
import javax.annotation.Resource

data class S3SourceMetadata(val etag: String) : MessageSourceWithGroupId {
   override val groupId = etag
}

@Component
class SqsS3SourceBuilder : PipelineSourceBuilder<AwsSqsS3TransportInputSpec> {
   private val formatDetector = FormatDetector.get(listOf(CsvFormatSpec))
   override val sourceType: PipelineSourceType = PipelineSourceType.Batch

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is AwsSqsS3TransportInputSpec
   }

   override fun getEmittedType(
      pipelineSpec: PipelineSpec<AwsSqsS3TransportInputSpec, *>,
      schema: Schema
   ): QualifiedName {
      return pipelineSpec.input.targetType.typeName
   }

   override fun buildBatch(
      pipelineSpec: PipelineSpec<AwsSqsS3TransportInputSpec, *>,
      inputType: Type?
   ): BatchSource<MessageContentProvider> {
      val csvModelFormatAnnotation = formatDetector.getFormatType(inputType!!)
         ?.let { if (it.second is CsvFormatSpec) CsvFormatSpecAnnotation.from(it.first) else null }
      return SourceBuilder.batch("sqs-s3-operation-poll") { context ->
         val sourceContext = PollingSqsOperationSourceContext(context.logger(), pipelineSpec, csvModelFormatAnnotation)
         sourceContext
      }
         .fillBufferFn { context: PollingSqsOperationSourceContext, buffer: SourceBuilder.SourceBuffer<MessageContentProvider> ->
            context.fill(buffer)
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

   var dataBuffer: LinkedBlockingQueue<Pair<MessageContentProvider, Long>> = LinkedBlockingQueue()

   @Resource
   lateinit var clock: Clock

   @Resource
   lateinit var connectionRegistry: AwsConnectionRegistry

   private var isDone = false


   fun fill(buffer: SourceBuilder.SourceBuffer<MessageContentProvider>) {
      val sink = mutableListOf<Pair<MessageContentProvider, Long>>()
      dataBuffer.drainTo(sink, 1024)
      if (sink.isNotEmpty()) {
         logger.info("Filling the SQS queue \"${inputSpec.queueName}\" with ${sink.size} items.")
         sink.forEach {
            buffer.add(it.first)
         }
      } else if (isDone) {
         buffer.close()
      }
   }

   private fun connection(): AwsConnectionConfiguration {
      return connectionRegistry.getConnection(inputSpec.connection)
   }

   private fun fetchSqsMessages(sqsClient: SqsClient): Pair<S3EventNotification, SqsMessageReceiptHandle>? {
      val messagesList = mutableListOf<Message>()
      try {
         val sqsRequest = ReceiveMessageRequest.builder()
            .queueUrl(inputSpec.queueName)
            .maxNumberOfMessages(1)
            .visibilityTimeout(12 * 60 * 60) // max permissable value is 12 hours.
            .build()

         messagesList.addAll(sqsClient.receiveMessage(sqsRequest).messages())
      } catch (e: Exception) {
         logger.log(Level.SEVERE, "Error in retrieving from the SQS queue \"${inputSpec.queueName}\".", e)
         return null
      }

      if (messagesList.isEmpty()) {
         logger.log(Level.INFO, "There are no messages in the SQS queue \"${inputSpec.queueName}\".")
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
         logger.log(Level.SEVERE, "Received an unexpected event from the \"${inputSpec.queueName}\".", e)
         deleteSqsMessage(sqsClient, firstMessage.receiptHandle())
         null
      }
   }

   private fun createSqsClient(): SqsClient {
      return SqsClient
         .builder()
         .configureWithExplicitValuesIfProvided(connection()).build()
   }

   private fun closeSqsClient(sqsClient: SqsClient) {
      try {
         sqsClient.close()
      } catch (e: Exception) {
         logger.log(Level.SEVERE, "Error in closing SQS client for \"${inputSpec.queueName}\"", e)
      }
   }

   private fun createS3Client(): S3Client {
      val connection = connection()
      val s3Builder = S3Client
         .builder()
         .configureWithExplicitValuesIfProvided(connection)
      return s3Builder.build()
   }

   private fun closeS3Client(s3Client: S3Client) {
      try {
         s3Client.close()
      } catch (e: Exception) {
         logger.log(Level.SEVERE, "Error in closing S3 client for \"${inputSpec.queueName}\".", e)
      }
   }

   private fun closeClients(s3Client: S3Client, sqsClient: SqsClient) {
      closeSqsClient(sqsClient)
      closeS3Client(s3Client)
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
            "Error in deleting a message with the receipt handle $receiptHandle from the SQS queue \"${inputSpec.queueName}\"",
            e
         )
      }
   }

   fun doWork() {
      val sqsClient = createSqsClient()
      val s3EventNotificationAndSqsReceiptHandler = fetchSqsMessages(sqsClient)
      if (s3EventNotificationAndSqsReceiptHandler == null) {
         closeSqsClient(sqsClient)
         return
      }

      val s3Client = createS3Client()
      val s3EventNotification = s3EventNotificationAndSqsReceiptHandler.first
      s3EventNotification.records.forEach {
         val bucketName = it.s3.bucket.name
         val objectKey = it.s3.`object`.key
         val etag = it.s3.`object`.geteTag().substring(0..12)
         logger.log(Level.INFO, "Fetching the object \"$objectKey\" from the S3 bucket \"$bucketName\".")
         try {
            val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(objectKey).build()
            val responseInputStream = s3Client.getObject(getObjectRequest)
            if (this.csvModelFormatAnnotation != null) {
               feedAsCsvRecord(responseInputStream, csvModelFormatAnnotation, etag)
            } else {
               val linesStream = BufferedReader(InputStreamReader(responseInputStream, StandardCharsets.UTF_8)).lines()
               linesStream.forEach { line ->
                  dataBuffer.add(Pair(StringContentProvider(line, S3SourceMetadata(etag)), clock.millis()))
               }
               isDone = true
            }
         } catch (e: Exception) {
            logger.log(
               Level.SEVERE,
               "Error in retrieving the S3 object \"$objectKey\" from the bucket \"$bucketName\".",
               e
            )
         }
      }

      deleteSqsMessage(sqsClient, s3EventNotificationAndSqsReceiptHandler.second)
      closeClients(s3Client, sqsClient)
   }

   private fun feedAsCsvRecord(
      inputStream: InputStream,
      csvModelFormatAnnotation: CsvFormatSpecAnnotation,
      etag: String
   ) {
      val csvFormat = CsvFormatFactory.fromParameters(csvModelFormatAnnotation.ingestionParameters)
      val parser = csvFormat.parse(inputStream.bufferedReader())
      parser.forEach { csvRecord ->
         dataBuffer.add(
            Pair(
               CsvRecordContentProvider(
                  csvRecord,
                  csvModelFormatAnnotation.ingestionParameters.nullValue,
                  S3SourceMetadata(etag)
               ), clock.millis()
            )
         )
      }
      isDone = true
   }

   @PostConstruct
   fun init() {
      doWork()
   }
}
