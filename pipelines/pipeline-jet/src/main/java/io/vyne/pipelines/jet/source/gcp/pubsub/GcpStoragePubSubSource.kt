package io.vyne.pipelines.jet.source.gcp.pubsub

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.cloud.storage.Blob
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.StreamSource
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.connectors.config.IConnectionsConfig
import io.vyne.connectors.config.gcp.GcpConnectionConfiguration
import io.vyne.models.csv.CsvFormatFactory
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.csv.CsvFormatSpecAnnotation
import io.vyne.models.format.FormatDetector
import io.vyne.pipelines.jet.api.transport.*
import io.vyne.pipelines.jet.api.transport.gcp.pubsub.GcpStoragePubSubTransportInputSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.PipelineSourceType
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import lang.taxi.utils.log
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.util.concurrent.LinkedBlockingQueue
import kotlin.io.path.inputStream

@Component
class GcpStoragePubSubSourceBuilder : PipelineSourceBuilder<GcpStoragePubSubTransportInputSpec> {
   private val formatDetector = FormatDetector.get(listOf(CsvFormatSpec))

   // TODO : This should be Stream, but I'm copying from the S3 one.
   override val sourceType: PipelineSourceType = PipelineSourceType.Stream

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is GcpStoragePubSubTransportInputSpec
   }

   override fun getEmittedType(
      pipelineSpec: PipelineSpec<GcpStoragePubSubTransportInputSpec, *>,
      schema: Schema
   ): QualifiedName {
      return pipelineSpec.input.targetTypeName.fqn()
   }

   override fun build(
      pipelineSpec: PipelineSpec<GcpStoragePubSubTransportInputSpec, *>,
      inputType: Type?
   ): StreamSource<MessageContentProvider>? {
      val csvModelFormatAnnotation = formatDetector.getFormatType(inputType!!)
         ?.let { if (it.second is CsvFormatSpec) CsvFormatSpecAnnotation.from(it.first) else null }

      return SourceBuilder.stream("gcp-pubsub-storage-stream") { context ->
         val pollingOperation = PollingPubSubOperation(context.logger(), pipelineSpec, csvModelFormatAnnotation)
         pollingOperation
      }.fillBufferFn { context: PollingPubSubOperation, buffer: SourceBuilder.SourceBuffer<MessageContentProvider> ->
         context.fill(buffer)
      }
         .build()
   }

   override fun buildBatch(
      pipelineSpec: PipelineSpec<GcpStoragePubSubTransportInputSpec, *>,
      inputType: Type?
   ): BatchSource<MessageContentProvider>? {
      val csvModelFormatAnnotation = formatDetector.getFormatType(inputType!!)
         ?.let { if (it.second is CsvFormatSpec) CsvFormatSpecAnnotation.from(it.first) else null }

      return SourceBuilder.batch("gcp-pubsub-storage-poll") { context ->
         val pollingOperation = PollingPubSubOperation(context.logger(), pipelineSpec, csvModelFormatAnnotation)
         pollingOperation
      }.fillBufferFn { context: PollingPubSubOperation, buffer: SourceBuilder.SourceBuffer<MessageContentProvider> ->
         context.fill(buffer)
      }
         .build()
   }
}

data class GcpBlobStoreMetadata(val etag: String) : MessageSourceWithGroupId {
   override val groupId = etag
}

@SpringAware
class PollingPubSubOperation(
   private val logger: ILogger,
   private val pipelineSpec: PipelineSpec<GcpStoragePubSubTransportInputSpec, *>,
   private val csvModelFormatAnnotation: CsvFormatSpecAnnotation?
) {
   private val inputSpec = pipelineSpec.input
   private val queuedMessages: LinkedBlockingQueue<MessageContentProvider> = LinkedBlockingQueue()

   private var isDone = false

   @Resource
   lateinit var connections: IConnectionsConfig

   private var subscriber: Subscriber? = null


   fun fill(buffer: SourceBuilder.SourceBuffer<MessageContentProvider>) {
      val sink = mutableListOf<MessageContentProvider>()
      queuedMessages.drainTo(sink, 1024)
      if (sink.isNotEmpty()) {
         logger.info("Consumed ${sink.size} items from GCP PubSub subscription'${inputSpec.subscriptionName}' .")
         sink.forEach { buffer.add(it) }
      }
   }

   private fun subscribeToEvents() {
      val subscriptionName = ProjectSubscriptionName.of(inputSpec.projectId, inputSpec.subscriptionName)
      val connection = connection()
      val credentials = GoogleCredentials.fromStream(connection.keyPath.inputStream())

      val storageService = StorageOptions.newBuilder()
         .setCredentials(credentials)
         .build()
         .service

      val receiver = MessageReceiver { message: PubsubMessage, consumer: AckReplyConsumer ->
         println("Received message: ${message.data.toStringUtf8()}")
         val bucket = message.attributesMap["bucketId"]
         val fileName = message.attributesMap["objectId"]

         val (blob, blobBytes) = loadBlob(storageService, bucket, fileName)
         val messages = if (this.csvModelFormatAnnotation != null) {
            feedAsCsvRecord(blobBytes, csvModelFormatAnnotation, blob.etag)
         } else {
            feedAsLines(blobBytes, blob.etag)
         }
         messages.forEach { queuedMessages.offer(it) }

         consumer.ack()
      }

      subscriber = Subscriber.newBuilder(subscriptionName, receiver)
         .setCredentialsProvider { credentials }
         .build()

      subscriber!!.startAsync().awaitRunning()
   }

   private fun loadBlob(
      storageService: Storage,
      bucket: String?,
      fileName: String?
   ): Pair<Blob, ByteArray> {
      try {
         val blob = storageService.get(bucket, fileName)
         val blobBytes = blob.getContent()
         return Pair(blob, blobBytes)
      } catch (e:Exception) {
         logger.severe(e)
         throw e
      }
   }

   private fun feedAsLines(blobBytes: ByteArray, etag: String): List<StringContentProvider> {
      val linesStream = BufferedReader(blobBytes.inputStream().bufferedReader()).lines()
      return linesStream.map { line ->
         StringContentProvider(line, GcpBlobStoreMetadata(etag))
      }.toList() ?: emptyList()
   }

   private fun feedAsCsvRecord(
      bytes: ByteArray,
      csvModelFormatAnnotation: CsvFormatSpecAnnotation,
      etag: String
   ): List<MessageContentProvider> {
      val csvFormat = CsvFormatFactory.fromParameters(csvModelFormatAnnotation.ingestionParameters)
      val parser = csvFormat.parse(bytes.inputStream().bufferedReader())
      val records = parser.map { csvRecord ->
         CsvRecordContentProvider(
            csvRecord,
            csvModelFormatAnnotation.ingestionParameters.nullValue,
            GcpBlobStoreMetadata(etag)
         )
      }
      isDone = true
      return records
   }

   private fun connection(): GcpConnectionConfiguration {
      return connections.googleCloud[inputSpec.connection] ?: error("No GCP connection named '${inputSpec.connection}' is defined")
//      return connections.getConnection(inputSpec.connection)
   }


   @PostConstruct
   fun init() {
      subscribeToEvents()
   }
}
