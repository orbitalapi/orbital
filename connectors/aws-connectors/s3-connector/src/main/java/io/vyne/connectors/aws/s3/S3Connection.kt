package io.vyne.connectors.aws.s3

import io.vyne.connectors.aws.core.configureWithExplicitValuesIfProvided
import io.vyne.models.csv.CsvFormatFactory
import io.vyne.models.csv.CsvFormatSpecAnnotation
import org.apache.commons.csv.CSVParser
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.S3Object
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Stream

class S3Connection(private val configuration: AwsS3ConnectionConnectorConfiguration) {
   private fun builder(): S3ClientBuilder {
      return S3Client
         .builder()
         .configureWithExplicitValuesIfProvided(configuration)
   }

   fun fetch(objectKey: String?): Stream<String> {
      val bucket = configuration.bucket
      val s3Client = builder().build()
      return s3Client.listObjectsV2Paginator {
         if (objectKey != null) it.bucket(bucket).prefix(objectKey) else it.bucket(bucket)
      }.contents().stream().map(S3Object::key).flatMap { s3ObjectKey ->
         val getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(s3ObjectKey).build()
         val inputStream = s3Client.getObject(getObjectRequest)
         BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
      }
   }

   fun fetchAsCsv(objectKey: String?, csvFormatSpecAnnotation: CsvFormatSpecAnnotation): Stream<CSVParser> {
      val csvFormat = CsvFormatFactory.fromParameters(csvFormatSpecAnnotation.ingestionParameters)
      val bucket = configuration.bucket
      val s3Client = builder().build()
      return s3Client.listObjectsV2Paginator {
         if (objectKey != null) it.bucket(bucket).prefix(objectKey) else it.bucket(bucket)
      }.contents()
         .stream()
         .map(S3Object::key)
         .flatMap { s3ObjectKey ->
            val getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(s3ObjectKey).build()
            val inputStream = s3Client.getObject(getObjectRequest)
            Stream.of(CSVParser.parse(inputStream, StandardCharsets.UTF_8, csvFormat))
         }
   }
}
