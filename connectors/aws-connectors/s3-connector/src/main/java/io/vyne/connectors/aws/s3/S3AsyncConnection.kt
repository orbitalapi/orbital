package io.vyne.connectors.aws.s3

import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.accessKey
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.secretKey
import reactor.core.publisher.Mono
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import java.time.Duration

object S3AsyncConnection {
   fun test(configuration: AwsConnectionConfiguration): Mono<List<String>> {
      val asyncHttpClient = NettyNioAsyncHttpClient.builder()
         .writeTimeout(Duration.ZERO)
         .maxConcurrency(64)
         .build()

      val serviceConfiguration = S3Configuration.builder()
         .checksumValidationEnabled(false)
         .chunkedEncodingEnabled(true)
         .build()

      val asyncS3Client = S3AsyncClient.builder()
         .httpClient(asyncHttpClient)
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(configuration.accessKey, configuration.secretKey)))
         .region(Region.of(configuration.region))
         .serviceConfiguration(serviceConfiguration)
         .build()

      return Mono.fromFuture(asyncS3Client.listBuckets().thenApply { bucketResponse -> bucketResponse.buckets().map { bucket -> bucket.name() } })
         .doOnTerminate { asyncS3Client.close() }
   }
}
