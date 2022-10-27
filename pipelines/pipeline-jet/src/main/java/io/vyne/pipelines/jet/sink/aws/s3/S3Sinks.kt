/**
 * This is the implementation from Hazelcast Jet that is modified to allow full control
 * of the filenames.
 */

/*
 * Copyright 2021 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vyne.pipelines.jet.sink.aws.s3

import com.hazelcast.function.FunctionEx
import com.hazelcast.function.SupplierEx
import com.hazelcast.internal.util.ExceptionUtil
import com.hazelcast.jet.core.Processor
import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.logging.ILogger
import com.hazelcast.memory.MemoryUnit
import com.hazelcast.spring.context.SpringAware
import io.vyne.models.TypedCollection
import io.vyne.models.TypedObject
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineAwareVariableProvider
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.schemas.Schema
import io.vyne.spring.VyneProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest
import software.amazon.awssdk.services.s3.model.UploadPartRequest
import java.nio.ByteBuffer
import java.nio.charset.Charset
import javax.annotation.Nonnull
import javax.annotation.Resource

/**
 * Contains factory methods for creating AWS S3 sinks.
 */
object S3Sinks {
   /**
    * Creates an AWS S3 [Sink] which writes items to files into the
    * given bucket. Sink converts each item to string using given `toStringFn` and writes it as a line. The sink creates a file
    * in the bucket for each processor instance. Name of the file will include
    * an user provided prefix (if defined) and processor's global index,
    * for example the processor having the
    * index 2 with prefix `my-object-` will create the object
    * `my-object-2`.
    *
    *
    * No state is saved to snapshot for this sink. If the job is restarted
    * previously written files will be overwritten.
    *
    *
    * The default local parallelism for this sink is 1.
    *
    *
    * Here is an example which reads from a map and writes the entries
    * to given bucket using [Object.toString] to convert the
    * values to a line.
    *
    * <pre>`Pipeline p = Pipeline.create();
    * p.readFrom(Sources.map("map"))
    * .writeTo(S3Sinks.s3("bucket", "my-map-", StandardCharsets.UTF_8,
    * () -> S3Client.create(),
    * Object::toString
    * ));
   `</pre> *
    *
    * @param bucketName     the name of the bucket
    * @param name           the name of the file
    * @param charset        the charset to be used when encoding the strings
    * @param clientSupplier S3 client supplier
    * @param toStringFn     the function which converts each item to its
    * string representation
   </T> */
   @Nonnull
   fun s3(
      @Nonnull bucketName: String?,
      name: String,
      @Nonnull pipelineName: String,
      @Nonnull charset: Charset,
      @Nonnull clientSupplier: SupplierEx<out S3Client>,
      @Nonnull toStringFn: FunctionEx<in Pair<MessageContentProvider, Schema>, String>
   ): Sink<in MessageContentProvider> {
      val charsetName = charset.name()
      return SinkBuilder
         .sinkBuilder(
            "s3Sink"
         ) { context: Processor.Context ->
            val sinkContext = context.managedContext().initialize(
               S3SinkContext(context.logger(), bucketName, name, pipelineName, charsetName, toStringFn, clientSupplier)
            )

            sinkContext as S3SinkContext
         }
         .receiveFn { obj: S3SinkContext, item: MessageContentProvider ->
            if (item is List<*>) {
               item.forEach { obj.receive(it as MessageContentProvider) }
            } else {
               obj.receive(
                  item
               )
            }
         }
         .flushFn { obj: S3SinkContext -> obj.flush() }
         .destroyFn { obj: S3SinkContext -> obj.close() }
         .build()
   }

   @SpringAware
   internal class S3SinkContext constructor(
      private val logger: ILogger,
      private val bucketName: String?,
      name: String,
      pipelineName: String,
      charsetName: String,
      toStringFn: FunctionEx<in Pair<MessageContentProvider, Schema>, String>,
      clientSupplier: SupplierEx<out S3Client>
   ) {

      @Resource
      lateinit var variableProvider: PipelineAwareVariableProvider

      @Resource
      lateinit var vyneProvider: VyneProvider

      fun schema(): Schema {
         return vyneProvider.createVyne().schema
      }

      private val name: String
      private val pipelineName: String
      private val s3Client: S3Client
      private val toStringFn: FunctionEx<in Pair<MessageContentProvider, Schema>, String>
      private val charset: Charset
      private val completedParts: MutableList<CompletedPart> = ArrayList()
      private var buffer: ByteBuffer? = null
      private var partNumber = MINIMUM_PART_NUMBER // must be between 1 and maximumPartNumber
      private var fileNumber = 0
      private var uploadId: String? = null
      private lateinit var key: String
      private var isHeaderWritten = false

      init {
         this.name = name
         this.pipelineName = pipelineName
         s3Client = clientSupplier.get()
         this.toStringFn = toStringFn
         charset = Charset.forName(charsetName)
         checkIfBucketExists()
         resizeBuffer(DEFAULT_MINIMUM_UPLOAD_PART_SIZE)
      }

      private fun initiateUpload() {
         this.logger.info("Creating Multipart upload request for bucket $bucketName and file Key $key")
         val req = CreateMultipartUploadRequest
            .builder()
            .bucket(bucketName)
            .key(key)
            .build()
         uploadId = s3Client.createMultipartUpload(req).uploadId()
         this.logger.info("Created Multipart upload request with uploadId => $uploadId for bucket $bucketName and file Key $key")
      }

      private fun checkIfBucketExists() {
         s3Client.getBucketLocation { b: GetBucketLocationRequest.Builder ->
            b.bucket(
               bucketName
            )
         }
      }

      fun receive(item: MessageContentProvider) {
         val doNeedToWriteHeader = item is TypedInstanceContentProvider && item.content !is TypedCollection
         val bytes = if (!isHeaderWritten && doNeedToWriteHeader) {
            isHeaderWritten = true
            val content = (item as TypedInstanceContentProvider).content
            val typedCollection =
               TypedCollection.arrayOf(content.type, listOf(content as TypedObject))
            toStringFn.apply(TypedInstanceContentProvider(typedCollection) to schema())
         } else {
            toStringFn.apply(item to schema())
         }.toByteArray(charset)
         val length = bytes.size

         // not enough space in buffer to write
         if (buffer!!.remaining() < length) {
            // we try to flush the current buffer first
            flush()
            // this might not be enough - either item is bigger than current
            // buffer size or there was not enough data in the buffer to upload
            // in this case we have to resize the buffer to hold more data
            if (buffer!!.remaining() < length) {
               resizeBuffer(length + buffer!!.position())
            }
         }
         buffer!!.put(bytes)
      }

      private fun resizeBuffer(minimumLength: Int) {
         assert(buffer == null || buffer!!.position() < minimumLength)
         val newCapacity = (minimumLength * BUFFER_SCALE).toInt()
         val newBuffer = ByteBuffer.allocateDirect(newCapacity)
         if (buffer != null) {
            buffer!!.flip()
            newBuffer.put(buffer)
         }
         buffer = newBuffer
      }

      fun flush() {
         if (uploadId == null) {
            key = variableProvider.getVariableProvider(pipelineName)
               .substituteVariablesInTemplateString(name)
            this.logger.info("Upload key for bucket $bucketName is $key")
            initiateUpload()
         }
         if (buffer!!.position() > DEFAULT_MINIMUM_UPLOAD_PART_SIZE) {
            val isLastPart = partNumber == maximumPartNumber
            flushBuffer(isLastPart)
         }
      }

      fun close() {
         s3Client.use {
            flushBuffer(true)
         }
      }

      private fun flushBuffer(isLastPart: Boolean) {
         if (buffer!!.position() > 0) {
            buffer!!.flip()
            this.logger.info("uploadPartRequest for bucket [$bucketName] and key [${key}] with upload Id [$uploadId] and part number [$partNumber]")
            val req = UploadPartRequest
               .builder()
               .bucket(bucketName)
               .key(key)
               .uploadId(uploadId)
               .partNumber(partNumber)
               .build()
            val eTag = s3Client.uploadPart(req, RequestBody.fromByteBuffer(buffer)).eTag()
            completedParts.add(CompletedPart.builder().partNumber(partNumber).eTag(eTag).build())
            partNumber++
            buffer!!.clear()
         }
         if (isLastPart) {
            completeUpload()
         }
      }

      private fun completeUpload() {
         try {
            if (completedParts.isEmpty()) {
               abortUpload()
            } else {
               this.logger.info("Completing the upload with id $uploadId to bucket $bucketName, object key $key")
               val req = CompleteMultipartUploadRequest
                  .builder()
                  .bucket(bucketName)
                  .key(key)
                  .uploadId(uploadId)
                  .multipartUpload { b: CompletedMultipartUpload.Builder ->
                     b.parts(
                        completedParts
                     )
                  }
                  .build()
               s3Client.completeMultipartUpload(req)
               completedParts.clear()
               partNumber = MINIMUM_PART_NUMBER
               uploadId = null
               fileNumber++
            }
         } catch (e: Exception) {
            abortUpload()
            ExceptionUtil.rethrow(e)
         }
      }

      private fun abortUpload() {
         this.logger.severe("Aborting the upload with id $uploadId to bucket $bucketName")
         if (uploadId != null) {
            // upload Id can be null if the sink is triggered with empy messages.
            s3Client.abortMultipartUpload { b: AbortMultipartUploadRequest.Builder ->
               b.uploadId(uploadId).bucket(
                  bucketName
               ).key(key)
            }
         }
      }

      companion object {
         const val DEFAULT_MAXIMUM_PART_NUMBER = 10000
         const val MINIMUM_PART_NUMBER = 1

         // visible for testing
         var maximumPartNumber = DEFAULT_MAXIMUM_PART_NUMBER

         // the minimum size required for each part in AWS multipart
         val DEFAULT_MINIMUM_UPLOAD_PART_SIZE = MemoryUnit.MEGABYTES.toBytes(5).toInt()
         const val BUFFER_SCALE = 1.2
      }
   }
}
