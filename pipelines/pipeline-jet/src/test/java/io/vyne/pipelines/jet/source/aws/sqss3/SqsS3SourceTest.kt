package io.vyne.pipelines.jet.source.aws.sqss3

import com.hazelcast.jet.core.JobStatus
import io.vyne.VersionedTypeReference
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.UTCClockProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.jet.awsConnection
import io.vyne.pipelines.jet.populateS3AndSns
import io.vyne.pipelines.jet.source.aws.sqss3.SqsS3SourceBuilder.Companion.NEXT_SCHEDULED_TIME_KEY
import org.awaitility.Awaitility
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.TimeUnit

@Testcontainers
@RunWith(SpringRunner::class)
class SqsS3SourceTest: BaseJetIntegrationTest() {
   val localStackImage = DockerImageName.parse("localstack/localstack").withTag("0.14.0")
   val bucket = "testbucket"
   val objectKey = "myfile"
   val sqsQueueName = "testqueue"
   var sqsQueueUrl = ""

   @JvmField
   @Rule
   var localstack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.SQS)


   @Before
   fun setUp() {
      sqsQueueUrl = populateS3AndSns(localstack, bucket, objectKey, sqsQueueName)
   }

   @Test
   fun `can read a csv file from s3`() {
      // Pipeline Kafka -> Direct
      // Date,Symbol,Open,High,Low,Close,Volume BTC,Volume USD
      val coinBaseSchema = """
type alias Price as Decimal
type alias Symbol as String
@io.vyne.formats.Csv(
            delimiter = ",",
            nullValue = "NULL"
         )
type OrderWindowSummary {
    symbol : Symbol by column("Symbol")
    open : Price by column("Open")
    // Added column
    high : Price by column("High")
    // Changed column
    close : Price by column("Close")
}""".trimIndent()
      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
         coinBaseSchema,
         emptyList(),
         listOf(localstack.awsConnection()),
         UTCClockProvider::class.java)
      applicationContext.getBean(AwsConnectionRegistry::class.java).register(localstack.awsConnection())
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "OrderWindowSummary")
      val pipelineSpec = PipelineSpec(
         name = "aws-s3-source",
         input = AwsSqsS3TransportInputSpec(
            localstack.awsConnection().connectionName,
            VersionedTypeReference.parse("OrderWindowSummary"),
            hashMapOf(),
            queueName = sqsQueueUrl,
            pollSchedule = CronExpressions.EVERY_SECOND,
            endPointOverride = localstack.getEndpointOverride(LocalStackContainer.Service.S3)
         ),
         output = outputSpec
      )

      val (pipeline,job) = startPipeline(jetInstance, vyneProvider, pipelineSpec)
      Awaitility.await().atMost(30, TimeUnit.SECONDS).until {
         job.status == JobStatus.RUNNING
      }

      Awaitility.await().atMost(30, TimeUnit.SECONDS).until {
         listSinkTarget.list.isNotEmpty()
      }
   }

}
