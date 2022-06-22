package io.vyne.pipelines.jet.source.aws.s3

import com.hazelcast.function.BiFunctionEx
import com.hazelcast.function.FunctionEx
import com.hazelcast.jet.Traverser
import com.hazelcast.jet.Traversers
import com.hazelcast.jet.Util
import com.hazelcast.jet.core.Processor
import com.hazelcast.jet.function.TriFunction
import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.SourceBuilder.SourceBuffer
import com.hazelcast.spring.context.SpringAware
import io.vyne.connectors.aws.core.accessKey
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.aws.core.secretKey
import io.vyne.pipelines.jet.BadRequestException
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportInputSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.PipelineSourceType
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.S3Object
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Stream
import javax.annotation.PostConstruct
import javax.annotation.Resource


@Component
class S3SourceBuilder : PipelineSourceBuilder<AwsS3TransportInputSpec> {

   override val sourceType: PipelineSourceType
      get() = PipelineSourceType.Batch

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is AwsS3TransportInputSpec
   }

   override fun buildBatch(
      pipelineSpec: PipelineSpec<AwsS3TransportInputSpec, *>,
      inputType: Type?
   ): BatchSource<MessageContentProvider> {
      val bucketName = pipelineSpec.input.bucket

      // Read CSV file as a stream of Strings
      val readFileFn: FunctionEx<InputStream, Stream<String>> =
         FunctionEx<InputStream, Stream<String>> { responseInputStream ->
            val reader = BufferedReader(InputStreamReader(responseInputStream, StandardCharsets.UTF_8))
            reader.lines()
         }

      // Map Each CSV file to a String Content.
      val mapFunc: BiFunctionEx<String, String, MessageContentProvider> =
         BiFunctionEx<String, String, MessageContentProvider>
         { _, line -> StringContentProvider(line) }


      // Map InputStream to String Stream.
      val adaptedFunction: TriFunction<InputStream, String, String, Stream<String>> =
         TriFunction<InputStream, String, String, Stream<String>>
         { inputStream, _, _ -> readFileFn.apply(inputStream) }

      return SourceBuilder.batch("s3-source") { context ->
         context.managedContext().initialize(
            VyneS3SourceContext(
               pipelineSpec,
               listOf(bucketName),
               pipelineSpec.input.objectKey,
               context,
               adaptedFunction,
               mapFunc
            )
         ) as VyneS3SourceContext


      }.fillBufferFn { sourceContext: VyneS3SourceContext, data: SourceBuffer<MessageContentProvider> ->
         sourceContext.fillBuffer(data)
      }.destroyFn { it.close() }
         .build()
   }


   override fun getEmittedType(pipelineSpec: PipelineSpec<AwsS3TransportInputSpec, *>, schema: Schema): QualifiedName {
      return pipelineSpec.input.targetType.typeName
   }
}


@SpringAware
class VyneS3SourceContext(
   private val pipelineSpec: PipelineSpec<AwsS3TransportInputSpec, *>,
   private val bucketNames: List<String>,
   private val prefix: String,
   context: Processor.Context,
   private val readFileFn: TriFunction<InputStream, String, String, Stream<String>>,
   private val mapFn: BiFunctionEx<String, String, MessageContentProvider>
) {

   private val inputSpec: AwsS3TransportInputSpec = pipelineSpec.input
   private lateinit var amazonS3: S3Client
   private val processorIndex: Int = context.globalProcessorIndex()
   private val totalParallelism: Int = context.totalParallelism()
   private var objectIterator: Iterator<Map.Entry<String, String>>? = null
   private var itemTraverser: Traverser<String>? = null
   private var currentKey: String? = null

   @Resource
   lateinit var connectionRegistry: AwsConnectionRegistry

   private fun s3Client(): S3Client {
      if (!connectionRegistry.hasConnection(inputSpec.connectionName)) {
         throw BadRequestException("PipelineSpec ${pipelineSpec.id} refers to connection ${inputSpec.connectionName} which does not exist")
      }
      val awsConnection = connectionRegistry.getConnection(inputSpec.connectionName)
      val builder = S3Client
         .builder()
         .credentialsProvider(
            StaticCredentialsProvider.create(
               AwsBasicCredentials.create(
                  awsConnection.accessKey,
                  awsConnection.secretKey
               )
            )
         )
         .region(Region.of(awsConnection.region))

      if (inputSpec.endPointOverride != null) {
         builder.endpointOverride(inputSpec.endPointOverride)
      }

      return builder.build()
   }

   fun fillBuffer(buffer: SourceBuffer<MessageContentProvider>) {
      if (itemTraverser != null) {
         addBatchToBuffer(buffer)
      } else {
         if (objectIterator!!.hasNext()) {
            val (bucketName, key) = objectIterator!!.next()
            val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(key).build() as GetObjectRequest
            val responseInputStream = amazonS3.getObject(getObjectRequest)
            currentKey = key
            itemTraverser = Traversers.traverseStream(readFileFn.apply(responseInputStream, key, bucketName))
            addBatchToBuffer(buffer)
         } else {
            buffer.close()
            objectIterator = null
         }
      }
   }

   private fun addBatchToBuffer(buffer: SourceBuffer<MessageContentProvider>) {
      assert(currentKey != null) { "currentKey must not be null" }
      for (i in 0..BATCH_COUNT) {
         val item = itemTraverser!!.next()
         if (item == null) {
            itemTraverser = null
            currentKey = null
            return
         }
         buffer.add(mapFn.apply(currentKey, item))
      }
   }

   private fun belongsToThisProcessor(key: String): Boolean {
      return Math.floorMod(key.hashCode(), totalParallelism) == processorIndex
   }

   fun close() {
      amazonS3.close()
   }

   companion object {
      private const val BATCH_COUNT = 1024
   }

   @PostConstruct
   fun init() {
      amazonS3 = s3Client()
      objectIterator = bucketNames.stream().flatMap { bucket: String ->
         amazonS3.listObjectsV2Paginator { b: ListObjectsV2Request.Builder ->
            b.bucket(bucket).prefix(prefix)
         }
            .contents()
            .stream()
            .map { obj: S3Object ->
               obj.key()
            }
            .filter { key: String -> belongsToThisProcessor(key) }
            .map { key: String -> Util.entry(bucket, key) }
      }.iterator()
   }
}
