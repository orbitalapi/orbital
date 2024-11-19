package com.orbitalhq.cockpit.core.connectors.aws

import com.orbitalhq.cockpit.core.connectors.getConnectionConfigOrThrowNotFound
import com.orbitalhq.connectors.aws.configureWithExplicitValuesIfProvided
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.aws.s3.S3Connection
import com.orbitalhq.connectors.aws.s3.buildAsyncS3Client
import com.orbitalhq.connectors.aws.s3.buildS3Client
import com.orbitalhq.connectors.aws.s3.buildS3Connection
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.NotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest
import software.amazon.awssdk.services.s3.model.ListBucketsRequest

@RestController
class S3ConnectorService(val registry: AwsConnectionRegistry) {
   @PreAuthorize("hasAuthority('${VynePrivileges.TestConnections}')")
   @GetMapping("/api/packages/{packageUri}/connections/aws/{connectionName}/s3/buckets")
   fun listBuckets(@PathVariable("connectionName") connectionName: String): Mono<ListBucketsResponse> {
      val (connection, client) = getS3Client(connectionName)
      return Mono.fromFuture(client.listBuckets(ListBucketsRequest.builder()
         .bucketRegion(connection.region)
         .build()
      ))
         .map { listBucketsResponse ->
            val names = listBucketsResponse.buckets().map { bucket ->
               bucket.name()
            }
            ListBucketsResponse(names)
         }
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.TestConnections}')")
   @GetMapping("/api/packages/{packageUri}/connections/aws/{connectionName}/s3/buckets/{bucketName}")
   fun listFilenames(
      @PathVariable("connectionName") connectionName: String,
      @PathVariable("bucketName") bucketName: String,
      @RequestParam("pattern") pattern: String,
   ): Mono<ListObjectsResponse> {
      val connectionConfig = registry.getConnectionConfigOrThrowNotFound(connectionName)
      val s3Connection = connectionConfig.buildS3Connection(bucketName)
      return s3Connection.listMatchingObjects(pattern)
         .map { s3Object ->
            s3Object.key()
         }
         .collectList()
         .map { names -> ListObjectsResponse(names) }

   }

   private fun getS3Client(connectionName: String): Pair<AwsConnectionConfiguration,S3AsyncClient> {
      val connection = registry.getConnectionConfigOrThrowNotFound(connectionName)
      val client = connection.buildAsyncS3Client()
      return connection to client
   }
}


data class ListBucketsResponse(
   val bucketNames: List<String>
)

data class ListObjectsResponse(
   val objectNames: List<String>
)
