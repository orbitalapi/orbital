package com.orbitalhq.connectors.aws.s3

import com.orbitalhq.connectors.aws.configureWithExplicitValuesIfProvided
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.formats.csv.CsvFormatFactory
import com.orbitalhq.formats.csv.CsvFormatSpecAnnotation
import org.apache.commons.csv.CSVParser
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.CompletedUpload
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

class S3Connection(private val configuration: AwsConnectionConfiguration, private val bucketName: String) {
   private fun builder(): S3ClientBuilder {
      return S3Client
         .builder()
         .configureWithExplicitValuesIfProvided(configuration)
   }

   fun write(filename: String, contents: Any):Mono<CompletedUpload> {
      val client = S3AsyncClient
         .builder()
         .configureWithExplicitValuesIfProvided(configuration)
         .build()
      val transferManager = S3TransferManager.builder()
         .s3Client(client)
         .build()

      val requestBody = when (contents) {
         is String -> AsyncRequestBody.fromString(contents)
         is ByteArray -> AsyncRequestBody.fromBytes(contents)
         else -> error("Don't know how to construct a request to S3 for type ${contents::class.simpleName}")
      }
      val putRequest = PutObjectRequest.builder()
         .bucket(bucketName)
         .key(filename)
         .build()
      val uploadRequest = UploadRequest.builder()
         .putObjectRequest(putRequest)
         .requestBody(requestBody)
         .build()
      return Mono.fromFuture(transferManager.upload(uploadRequest).completionFuture())
   }

   fun hasPattern(filePattern: String): Boolean {
      return filePattern.contains("*")
   }

   private fun isPrefixPattern(filePattern: String): Boolean {
      return filePattern.endsWith("*")
   }

   fun fetchAsInputStream(objectKey: String?): Flux<InputStream> {
      val s3Client = builder().build()
      val hasPattern = objectKey != null && hasPattern(objectKey)
      val pathMatcher = if (hasPattern) {
         FileSystems.getDefault().getPathMatcher("glob:${objectKey}")
      } else null
      return Flux.create<InputStream> { emitter ->
         s3Client.listObjectsV2Paginator { builder ->
            val bucketBuilder = builder.bucket(bucketName)
            when {
               objectKey != null && isPrefixPattern(objectKey) -> {
                  bucketBuilder.prefix(objectKey.removeSuffix("*"))
               }
               objectKey != null && hasPattern(objectKey) -> bucketBuilder // nothing to do here, as we have to filter keys individually below
               objectKey != null -> bucketBuilder.prefix(objectKey)
               else -> bucketBuilder
            }
         }.contents()
            .filter { s3Object ->
               when {
                  hasPattern -> {
                     pathMatcher!!.matches(Paths.get(s3Object.key()))
                  }
                  objectKey == null -> true
                  else -> s3Object.key() == objectKey
               }
            }
            .map { s3Object ->
               val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(s3Object.key()).build()
               val inputStream = s3Client.getObject(getObjectRequest)
               emitter.next(inputStream)
            }
         emitter.complete()
      }
         .publishOn(Schedulers.boundedElastic())

   }

   fun fetch(objectKey: String?): Stream<String> {
      val s3Client = builder().build()
      return s3Client.listObjectsV2Paginator {
         if (objectKey != null) it.bucket(bucketName).prefix(objectKey) else it.bucket(bucketName)
      }.contents().stream().map(S3Object::key).flatMap { s3ObjectKey ->
         val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build()
         val inputStream = s3Client.getObject(getObjectRequest)
         BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
      }
   }

   fun fetchAsCsv(objectKey: String?, csvFormatSpecAnnotation: CsvFormatSpecAnnotation): Stream<CSVParser> {
      val csvFormat = CsvFormatFactory.fromParameters(csvFormatSpecAnnotation.ingestionParameters)
      val s3Client = builder().build()
      return s3Client.listObjectsV2Paginator {
         if (objectKey != null) it.bucket(bucketName).prefix(objectKey) else it.bucket(bucketName)
      }.contents()
         .stream()
         .map(S3Object::key)
         .flatMap { s3ObjectKey ->
            val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(s3ObjectKey).build()
            val inputStream = s3Client.getObject(getObjectRequest)
            Stream.of(CSVParser.parse(inputStream, StandardCharsets.UTF_8, csvFormat))
         }
   }
}
