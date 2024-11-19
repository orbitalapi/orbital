package com.orbitalhq.connectors.aws.s3

import com.orbitalhq.connectors.aws.configureWithExplicitValuesIfProvided
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.CompletedUpload
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Paths

class S3Connection(private val configuration: AwsConnectionConfiguration, private val bucketName: String) {
   private fun clientBuilder(): S3ClientBuilder {
      return S3Client
         .builder()
         .configureWithExplicitValuesIfProvided(configuration)
   }

   private fun asyncClientBuilder(): S3AsyncClientBuilder {
      return S3AsyncClient.builder()
         .configureWithExplicitValuesIfProvided(configuration)
   }

   fun write(filename: String, contents: Any): Mono<CompletedUpload> {
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

   fun listMatchingObjects(filePattern: String?): Flux<S3Object> {
      val s3Client = asyncClientBuilder().build()
      val hasPattern = filePattern != null && hasPattern(filePattern)
      val pathMatcher = if (hasPattern) {
         FileSystems.getDefault().getPathMatcher("glob:${filePattern}")
      } else null
      val asyncBucketList = s3Client.listObjectsV2Paginator { builder ->
         val bucketBuilder = builder.bucket(bucketName)
         when {
            filePattern != null && isPrefixPattern(filePattern) -> {
               bucketBuilder.prefix(filePattern.removeSuffix("*"))
            }

            filePattern != null && hasPattern(filePattern) -> bucketBuilder // nothing to do here, as we have to filter keys individually below
            filePattern != null -> bucketBuilder.prefix(filePattern)
            else -> bucketBuilder
         }
      }.contents()
         .filter { s3Object ->
            when {
               hasPattern -> {
                  pathMatcher!!.matches(Paths.get(s3Object.key()))
               }

               filePattern == null -> true
               else -> s3Object.key() == filePattern
            }
         }
      return Flux.from(asyncBucketList)
   }

   fun fetchAsInputStream(objectKey: String?): Flux<Pair<S3Object, Mono<InputStream>>> {
      val s3Client = clientBuilder().build()
      return listMatchingObjects(objectKey)
         .map { s3Object ->
            val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(s3Object.key()).build()
            val deferredInputStream = Mono.defer {
               val result = s3Client.getObject(getObjectRequest) as InputStream
               Mono.just(result)
            }
            s3Object to deferredInputStream
         }
         .publishOn(Schedulers.boundedElastic())

   }
}

fun AwsConnectionConfiguration.buildAsyncS3Client(): S3AsyncClient {
   val client = S3AsyncClient.builder().configureWithExplicitValuesIfProvided(this).build()
   return client
}

fun AwsConnectionConfiguration.buildS3Client(): S3Client {
   val client = S3Client.builder().configureWithExplicitValuesIfProvided(this).build()
   return client
}

fun AwsConnectionConfiguration.buildS3Connection(bucketName: String): S3Connection {
   return S3Connection(this, bucketName)
}
