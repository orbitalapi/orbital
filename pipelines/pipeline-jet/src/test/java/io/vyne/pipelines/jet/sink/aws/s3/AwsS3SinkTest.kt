package io.vyne.pipelines.jet.sink.aws.s3

import com.winterbe.expekt.should
import io.vyne.connectors.aws.core.AwsConnection
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportOutputSpec
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.schemas.fqn
import org.apache.commons.io.IOUtils
import org.awaitility.Awaitility
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class AwsS3SinkTest : BaseJetIntegrationTest() {
   val localStackImage: DockerImageName = DockerImageName.parse("localstack/localstack").withTag("0.14.0")
   val bucket = "testbucket"
   val objectKey = "example.txt"

   @JvmField
   @Rule
   val localstack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.S3)

   lateinit var s3: S3Client
   lateinit var awsConnectionConfig: AwsConnectionConfiguration

   @Before
   fun setUp() {
      s3 = S3Client
         .builder()
         .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
         .credentialsProvider(
            StaticCredentialsProvider.create(
               AwsBasicCredentials.create(
                  localstack.accessKey, localstack.secretKey
               )
            )
         )
         .region(Region.of(localstack.region))
         .build()
      s3.createBucket { b: CreateBucketRequest.Builder -> b.bucket(bucket) }
      awsConnectionConfig = AwsConnectionConfiguration(
         connectionName = "test-aws",
         mapOf(
            AwsConnection.Parameters.ACCESS_KEY.templateParamName to localstack.accessKey,
            AwsConnection.Parameters.SECRET_KEY.templateParamName to localstack.secretKey,
            AwsConnection.Parameters.AWS_REGION.templateParamName to localstack.region,
            AwsConnection.Parameters.ENDPOINT_OVERRIDE.templateParamName to localstack.getEndpointOverride(
               LocalStackContainer.Service.S3
            ).toString()
         )
      )
   }

   @Test
   fun `can submit to AWS S3`() {
      // TODO This shouldn't be needed as we should use Spring DI as set up in the setUp method above
      awsConnectionRegistry.register(awsConnectionConfig)

      val (jetInstance, _, vyneProvider) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }

         @io.vyne.formats.Csv(
            delimiter = "|",
            nullValue = "NULL"
         )
         model Target {
            givenName : FirstName
         }
      """, awsConnections = listOf(awsConnectionConfig)
      )

      val pipelineSpec = PipelineSpec(
         "test-aws-s3-sink",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn()
         ),
         output = AwsS3TransportOutputSpec(
            "test-aws",
            bucket,
            objectKey,
            "Target"
         )
      )

      startPipeline(jetInstance, vyneProvider, pipelineSpec)
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         s3.listObjectsV2 { it.bucket(bucket) }.contents().any { it.key() == objectKey }
      }
      val contents = IOUtils.toString(s3.getObject { it.bucket(bucket).key(objectKey) }, StandardCharsets.UTF_8)
      contents.should.equal("""foobar""")
   }
}
