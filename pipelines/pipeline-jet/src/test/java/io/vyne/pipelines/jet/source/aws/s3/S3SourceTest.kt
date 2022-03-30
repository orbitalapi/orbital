package io.vyne.pipelines.jet.source.aws.s3

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportInputSpec
import io.vyne.pipelines.jet.awsConnection
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


@Testcontainers
@RunWith(SpringRunner::class)
class S3SourceTest: BaseJetIntegrationTest() {
   val localStackImage = DockerImageName.parse("localstack/localstack").withTag("0.14.0")
   val bucket = "testbucket"
   val objectKey = "myfile"

   @JvmField
   @Rule
   var localstack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.S3)

   @Before
   fun setUp() {
      val s3: S3Client = S3Client
         .builder()
         .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
            localstack.accessKey, localstack.secretKey
         )))
         .region(Region.of(localstack.region))
         .build()

      val resource = Resources.getResource("Coinbase_BTCUSD_3rows.csv").path
      s3.createBucket { b: CreateBucketRequest.Builder -> b.bucket(bucket) }
      s3.putObject({ builder -> builder.bucket(bucket).key(objectKey) }, Paths.get(resource))
   }

   @Test
   fun `can read a csv file from s3`() {
      // Pipeline Kafka -> Direct
      val coinBaseSchema = """
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by column(2)
    open : Price by column(3)
    // Added column
    high : Price by column(4)
    // Changed column
    close : Price by column(6)
}""".trimIndent()
      val awsConnection = localstack.awsConnection()
      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(coinBaseSchema, emptyList(), listOf(awsConnection))
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "OrderWindowSummary")
      val pipelineSpec = PipelineSpec(
         name = "aws-s3-source",
         input = AwsS3TransportInputSpec(
            connection = localstack.awsConnection().connectionName,
            bucket = bucket,
            objectKey = objectKey,
            VersionedTypeReference.parse("OrderWindowSummary"),
            emptyMap(),
            endPointOverride = localstack.getEndpointOverride(LocalStackContainer.Service.S3)
         ),
         output = outputSpec
      )

      val (_,job) = startPipeline(jetInstance = jetInstance, vyneProvider = vyneProvider, pipelineSpec = pipelineSpec, validateJobStatusEventually = false)
      job.future.get(10, TimeUnit.SECONDS)
      listSinkTarget.size.should.equal(4)
   }

}
