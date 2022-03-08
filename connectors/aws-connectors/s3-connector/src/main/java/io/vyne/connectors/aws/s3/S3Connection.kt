package io.vyne.connectors.aws.s3

import io.vyne.connectors.aws.core.accessKey
import io.vyne.connectors.aws.core.endPointOverride
import io.vyne.connectors.aws.core.region
import io.vyne.connectors.aws.core.secretKey
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.S3Object
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.stream.Stream

class S3Connection(private val configuration: AwsS3ConnectionConnectorConfiguration) {
   private fun builder(): S3ClientBuilder {
       val builder =  S3Client
         .builder()
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(configuration.accessKey,
            configuration.secretKey)))
         .region(Region.of(configuration.region))

      if (configuration.endPointOverride != null) {
         builder.endpointOverride(URI.create(configuration.endPointOverride))
      }

      return builder
   }

   fun fetch(objectKey: String?): Stream<String> {
      val bucket = configuration.bucket
      val s3Client = builder().build()
      return  s3Client.listObjectsV2Paginator {
         if (objectKey != null) it.bucket(bucket).prefix(objectKey) else it.bucket(bucket)
      }.contents().stream().map(S3Object::key).flatMap { s3ObjectKey ->
         val getObjectRequest =  GetObjectRequest.builder().bucket(bucket).key(s3ObjectKey).build()
         val inputStream = s3Client.getObject(getObjectRequest)
         BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
      }
   }

   fun fetchAsCsv(objectKey: String?): Stream<CSVParser> {
      val bucket = configuration.bucket
      val s3Client = builder().build()
      return s3Client.listObjectsV2Paginator {
         if (objectKey != null) it.bucket(bucket).prefix(objectKey) else it.bucket(bucket)
      }.contents()
         .stream()
         .map(S3Object::key)
         .flatMap { s3ObjectKey ->
         val getObjectRequest =  GetObjectRequest.builder().bucket(bucket).key(s3ObjectKey).build()
         val inputStream = s3Client.getObject(getObjectRequest)
         Stream.of(CSVParser.parse(inputStream, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader()))
      }

   }
}
