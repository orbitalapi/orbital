package io.vyne.pipelines.jet.source.aws.sqss3

import io.vyne.connectors.aws.core.AwsConnection
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.RatingReport
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.jet.awsConnection
import mu.KotlinLogging
import org.awaitility.Awaitility
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import java.time.Duration
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger { }

/**
 * This test is ignored as it is supposed to run against actual Aws.
 */
@Ignore
@RunWith(SpringRunner::class)
class SqsS3SourceAwsTest : BaseJetIntegrationTest() {
   private val sqsQueueName = "http://localhost:4566/000000000000/hsbc-esgratings-scores-notification"

   // Set accessKey and secretKey accordingly!
   private val awsConnection = AwsConnectionConfiguration("aws-test-connection",
      mapOf(AwsConnection.Parameters.ACCESS_KEY.templateParamName to "xxx",
         AwsConnection.Parameters.SECRET_KEY.templateParamName to "yyy",
         AwsConnection.Parameters.AWS_REGION.templateParamName to "eu-west-2")
   )


   @Test
   fun `can read a csv file from s3`() {
      // Pipeline Kafka -> Direct

      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(RatingReport.ratingsSchema(), emptyList(), listOf(awsConnection))
      applicationContext.getBean(AwsConnectionRegistry::class.java).register(awsConnection)
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "RatingsReport")
      val pipelineSpec = PipelineSpec(
         name = "aws-s3-source",
         input = AwsSqsS3TransportInputSpec(
            awsConnection.connectionName,
            RatingReport.versionedType,
            hashMapOf(),
            queueName = sqsQueueName,
            pollSchedule = CronExpressions.EVERY_SECOND
         ),
         output = outputSpec
      )

      val (pipeline, job) = startPipeline(jetInstance, vyneProvider, pipelineSpec)
      // Wait until the next scheduled time is set
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         val metrics = job.metrics
         val nextScheduledTime = metrics.get(SqsS3SourceBuilder.NEXT_SCHEDULED_TIME_KEY)
         nextScheduledTime.isNotEmpty()
      }

      applicationContext.moveTimeForward(Duration.ofSeconds(2))
      Awaitility.await().atMost(30, TimeUnit.SECONDS).until {
         listSinkTarget.list.isNotEmpty()
      }
   }
}
