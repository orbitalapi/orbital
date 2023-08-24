package io.vyne.connectors.aws.s3

import io.vyne.connectors.aws.configureWithExplicitValuesIfProvided
import io.vyne.connectors.config.aws.AwsConnectionConfiguration
import reactor.core.publisher.Mono
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
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
         .configureWithExplicitValuesIfProvided(configuration)
         .serviceConfiguration(serviceConfiguration)
         .build()

      return Mono.fromFuture(
         asyncS3Client.listBuckets()
            .thenApply { bucketResponse -> bucketResponse.buckets().map { bucket -> bucket.name() } })
         .doOnTerminate { asyncS3Client.close() }
   }
}
