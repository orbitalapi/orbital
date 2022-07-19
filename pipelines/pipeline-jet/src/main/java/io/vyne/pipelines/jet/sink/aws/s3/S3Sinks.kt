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
import com.hazelcast.internal.util.StringUtil
import com.hazelcast.jet.core.Processor
import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.memory.MemoryUnit
import com.hazelcast.spring.context.SpringAware
import io.vyne.pipelines.jet.api.transport.PipelineAwareVariableProvider
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
    * @param <T>            type of the items the sink accepts
    * @param bucketName     the name of the bucket
    * @param prefix         the prefix to be included in the file name
    * @param charset        the charset to be used when encoding the strings
    * @param clientSupplier S3 client supplier
    * @param toStringFn     the function which converts each item to its
    * string representation
   </T> */
   @Nonnull
   fun <T> s3(
      @Nonnull bucketName: String?,
      prefix: String?,
      @Nonnull pipelineName: String,
      @Nonnull charset: Charset,
      @Nonnull clientSupplier: SupplierEx<out S3Client>,
      @Nonnull toStringFn: FunctionEx<in Pair<T, Schema>, String>
   ): Sink<in T> {
      val charsetName = charset.name()
      return SinkBuilder
         .sinkBuilder(
            "s3Sink"
         ) { context: Processor.Context ->
            val sinkContext = context.managedContext().initialize(
               S3SinkContext(
                  bucketName, prefix, pipelineName, charsetName, context.globalProcessorIndex(),
                  toStringFn, clientSupplier
               )
            )

            sinkContext as S3SinkContext<T>
         }
         .receiveFn { obj: S3SinkContext<T>, item: T ->
            obj.receive(
               item
            )
         }
         .flushFn { obj: S3SinkContext<T> -> obj.flush() }
         .destroyFn { obj: S3SinkContext<T> -> obj.close() }
         .build()
   }

   @SpringAware
   internal class S3SinkContext<T> constructor(
      private val bucketName: String?,
      prefix: String?,
      pipelineName: String,
      charsetName: String, processorIndex: Int,
      toStringFn: FunctionEx<in Pair<T, Schema>, String>,
      clientSupplier: SupplierEx<out S3Client>
   ) {

      @Resource
      lateinit var variableProvider: PipelineAwareVariableProvider

      @Resource
      lateinit var vyneProvider: VyneProvider

      fun schema(): Schema {
         return vyneProvider.createVyne().schema
      }

      private val prefix: String
      private val pipelineName: String
      private val processorIndex: Int
      private val s3Client: S3Client
      private val toStringFn: FunctionEx<in Pair<T, Schema>, String>
      private val charset: Charset
      private val lineSeparatorBytes: ByteArray
      private val completedParts: MutableList<CompletedPart> = ArrayList()
      private var buffer: ByteBuffer? = null
      private var partNumber = MINIMUM_PART_NUMBER // must be between 1 and maximumPartNumber
      private var fileNumber = 0
      private var uploadId: String? = null

      init {
         val trimmedPrefix = StringUtil.trim(prefix)
         this.prefix = if (StringUtil.isNullOrEmpty(trimmedPrefix)) "" else trimmedPrefix
         this.pipelineName = pipelineName
         this.processorIndex = processorIndex
         s3Client = clientSupplier.get()
         this.toStringFn = toStringFn
         charset = Charset.forName(charsetName)
         lineSeparatorBytes = System.lineSeparator().toByteArray(charset)
         checkIfBucketExists()
         resizeBuffer(DEFAULT_MINIMUM_UPLOAD_PART_SIZE)
      }

      private fun initiateUpload() {
         val req = CreateMultipartUploadRequest
            .builder()
            .bucket(bucketName)
            .key(key())
            .build()
         uploadId = s3Client.createMultipartUpload(req).uploadId()
      }

      private fun checkIfBucketExists() {
         s3Client.getBucketLocation { b: GetBucketLocationRequest.Builder ->
            b.bucket(
               bucketName
            )
         }
      }

      fun receive(item: T) {
         val bytes = toStringFn.apply(item to schema()).toByteArray(charset)
         val length = bytes.size + lineSeparatorBytes.size

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
         buffer!!.put(lineSeparatorBytes)
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
            initiateUpload()
         }
         if (buffer!!.position() > DEFAULT_MINIMUM_UPLOAD_PART_SIZE) {
            val isLastPart = partNumber == maximumPartNumber
            flushBuffer(isLastPart)
         }
      }

      fun close() {
         try {
            flushBuffer(true)
         } finally {
            s3Client.close()
         }
      }

      private fun flushBuffer(isLastPart: Boolean) {
         if (buffer!!.position() > 0) {
            buffer!!.flip()
            val req = UploadPartRequest
               .builder()
               .bucket(bucketName)
               .key(key())
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
               val req = CompleteMultipartUploadRequest
                  .builder()
                  .bucket(bucketName)
                  .key(key())
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
         s3Client.abortMultipartUpload { b: AbortMultipartUploadRequest.Builder ->
            b.uploadId(uploadId).bucket(
               bucketName
            ).key(key())
         }
      }

      private fun key(): String {
         return variableProvider.getVariableProvider(pipelineName)
            .substituteVariablesInTemplateString(prefix)
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
