package io.vyne.pipelines.jet.source.aws.s3

import com.winterbe.expekt.should
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportInputSpec
import io.vyne.pipelines.jet.awsConnection
import io.vyne.utils.toPath
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
import java.util.concurrent.TimeUnit


@Testcontainers
@RunWith(SpringRunner::class)
class S3SourceTest : BaseJetIntegrationTest() {
   val localStackImage = DockerImageName.parse("localstack/localstack").withTag("1.0.4")
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
         .region(Region.of(localstack.region))
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("not-used", "not-used")))
         .build()

      s3.createBucket { b: CreateBucketRequest.Builder -> b.bucket(bucket) }
      s3.putObject({ builder -> builder.bucket(bucket).key(objectKey) }, "Coinbase_BTCUSD_3rows.csv".toPath())
   }

   @Test
   fun `can read a csv file from s3`() {
      // Pipeline Kafka -> Direct
      val coinBaseSchema = """
type alias Price as Decimal
type alias Symbol as String
model OrderWindowSummary {
    symbol : Symbol by column("Symbol")
    open : Price by column("Open")
    // Added column
    high : Price by column("High")
    // Changed column
    close : Price by column("Close")
}""".trimIndent()
      val awsConnection = localstack.awsConnection()
      val (hazelcastInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
         coinBaseSchema,
         emptyList(),
         listOf(awsConnection)
      )
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "OrderWindowSummary")
      val pipelineSpec = PipelineSpec(
         name = "aws-s3-source",
         input = AwsS3TransportInputSpec(
            connectionName = localstack.awsConnection().connectionName,
            bucket = bucket,
            objectKey = objectKey,
            targetTypeName = "OrderWindowSummary"
         ),
         outputs = listOf(outputSpec)
      )

      val (_, job) = startPipeline(
         hazelcastInstance = hazelcastInstance,
         vyneProvider = vyneProvider,
         pipelineSpec = pipelineSpec,
         validateJobStatusIsRunningEventually = false
      )
      job!!.future.get(10, TimeUnit.SECONDS)
      listSinkTarget.size.should.equal(5)
   }

}
