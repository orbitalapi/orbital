package io.vyne.connectors.aws.s3

import io.vyne.connectors.aws.core.accessKey
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.secretKey
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.nio.ByteBuffer
import java.time.Duration

class S3AsyncConnection(private val configuration: AwsS3ConnectionConnectorConfiguration) {
   private val asyncHttpClient = NettyNioAsyncHttpClient.builder()
      .writeTimeout(Duration.ZERO)
      .maxConcurrency(64)
      .build()

   private val serviceConfiguration = S3Configuration.builder()
      .checksumValidationEnabled(false)
      .chunkedEncodingEnabled(true)
      .build()

   private val asyncS3Client = S3AsyncClient.builder()
      .httpClient(asyncHttpClient)
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(configuration.accessKey, configuration.secretKey)))
      .region(Region.of(configuration.region))
      .serviceConfiguration(serviceConfiguration)
      .build()

   fun test(): Mono<List<String>> {
      return Mono.fromFuture(asyncS3Client.listBuckets().thenApply { bucketResponse -> bucketResponse.buckets().map { bucket -> bucket.name() } })
   }

   fun objectStream(fileKey: String): Mono<Flux<ByteBuffer>?> {
      val getObjectRequest = GetObjectRequest.builder()
         .bucket(configuration.bucket)
         .key(fileKey)
         .build()

      val response = FluxResponseProvider()
      return Mono.fromFuture(asyncS3Client.getObject(getObjectRequest, response))
         .map { res -> res.flux }
   }
}
