package com.orbitalhq.connectors.aws.s3

import com.google.common.io.Resources
import com.orbitalhq.Vyne
import com.orbitalhq.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.models.format.DefaultFormatRegistry
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyne
import com.orbitalhq.testVyneWithStub
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import org.junit.Before
import org.junit.Rule
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.random.Random

@Testcontainers
abstract class BaseS3Test {

   private val localStackImage = DockerImageName.parse("localstack/localstack").withTag("3.0")
   @JvmField
   @Rule
   var localstack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.S3)

   private val connectionRegistry = AwsInMemoryConnectionRegistry()

   lateinit var s3Client: S3Client
   val AWS_CONNECTION_NAME = "TestAwsConnection"

   @Before
   fun before() {
      // Upload test CSV into S3
      s3Client = S3Client
         .builder()
         .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
         .region(Region.of(localstack.region))
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("not-used", "not-used")))
         .build()

      val connectionConfig = AwsConnectionConfiguration(
         connectionName = AWS_CONNECTION_NAME,
         region = localstack.region,
         accessKey = localstack.accessKey,
         secretKey = localstack.secretKey,
         endPointOverride = localstack.getEndpointOverride(
            LocalStackContainer.Service.S3
         ).toString()
      )
      connectionRegistry.register(connectionConfig)
   }

   fun createBucket(bucketName: String) {
      s3Client.createBucket { b: CreateBucketRequest.Builder -> b.bucket(bucketName) }
   }
   fun createBucketWithRandomName(prefix: String): String {
      val bucketName = "$prefix${Random.nextInt()}".toLowerCase()
      createBucket(bucketName)
      return bucketName
   }

   fun uploadResourceToS3(bucketName: String, filename: String, path: Path) {
      s3Client.putObject({ builder -> builder.bucket(bucketName).key(filename) }, path)
   }

   fun vyneWithS3Invoker(taxi: String): Pair<Vyne, StubService> {
      val schema = TaxiSchema.fromStrings(
         listOf(
            S3ConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            taxi
         )
      )
      val s3invoker = S3Invoker(connectionRegistry, SimpleSchemaProvider(schema), formatRegistry = DefaultFormatRegistry(
         listOf(CsvFormatSpec)
      ))
      val invokers = listOf(
         s3invoker
      )
      return testVyneWithStub(schema, invokers)
   }
}
