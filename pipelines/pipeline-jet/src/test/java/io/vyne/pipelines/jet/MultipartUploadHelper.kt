package io.vyne.pipelines.jet

import mu.KotlinLogging
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse
import software.amazon.awssdk.services.s3.model.UploadPartRequest
import software.amazon.awssdk.services.s3.model.UploadPartResponse
import java.io.ByteArrayOutputStream

private val log = KotlinLogging.logger {  }
class MultipartUploadHelper(private val s3Client: S3Client, private val bucket: String, private val destinationKey: String) {

   private var uploadId: String = ""
   private val parts = mutableListOf<CompletedPart>()

   fun start(): MultipartUploadHelper {
      isValidStart()
      val multipartUpload: CreateMultipartUploadResponse = s3Client
         .createMultipartUpload(CreateMultipartUploadRequest.builder()
            .bucket(bucket)
            .key(destinationKey)
            .build())

      uploadId = multipartUpload.uploadId()
      return this
   }

    fun partUpload(byteArrayOutputStream: ByteArrayOutputStream) {
      val partNumber = parts.size + 1
      val uploadPartResponse: UploadPartResponse = s3Client.uploadPart(UploadPartRequest.builder()
         .bucket(bucket)
         .key(destinationKey)
         .uploadId(uploadId)
         .partNumber(partNumber)
         .build(), RequestBody.fromBytes(byteArrayOutputStream.toByteArray()))
      parts.add(CompletedPart.builder()
         .partNumber(partNumber)
         .eTag(uploadPartResponse.eTag())
         .build())
      byteArrayOutputStream.reset()
   }

   fun complete(byteArrayOutputStream: ByteArrayOutputStream?) {
      partUpload(byteArrayOutputStream!!)
      s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
         .uploadId(uploadId)
         .bucket(bucket)
         .key(destinationKey)
         .multipartUpload(CompletedMultipartUpload.builder()
            .parts(parts).build())
         .build())
      log.info("Multipart Upload complete with " + parts.size + " parts")
   }



   fun isValidStart(): Boolean {
      if (uploadId.isEmpty()  && parts.isEmpty()) {
         return true;
      }
      throw  RuntimeException("Invalid Multipart Upload Start");
   }

}
