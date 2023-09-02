package com.orbitalhq.pipelines.jet.source.aws.sqss3

import com.orbitalhq.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.pipelines.jet.BaseJetIntegrationTest
import com.orbitalhq.pipelines.jet.RatingReport
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import com.orbitalhq.pipelines.jet.api.transport.http.CronExpressions
import org.awaitility.Awaitility
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.TimeUnit

/**
 * This test is ignored as it is supposed to run against actual Aws.
 */
@Ignore
@RunWith(SpringRunner::class)
class SqsS3SourceAwsTest : BaseJetIntegrationTest() {
   private val sqsQueueName = "http://localhost:4566/000000000000/hsbc-esgratings-scores-notification"

   // Set accessKey and secretKey accordingly!
   private val awsConnection = AwsConnectionConfiguration(
      "aws-test-connection",
      region = "eu-west-2",
      accessKey = "not-used",
      secretKey = "not-used",
   )


   @Test
   fun `can read a csv file from s3`() {
      // Pipeline Kafka -> Direct

      val (hazelcastInstance, applicationContext, vyneClient) = jetWithSpringAndVyne(
         RatingReport.ratingsSchema(),
         emptyList(),
         listOf(awsConnection)
      )
      applicationContext.getBean(AwsInMemoryConnectionRegistry::class.java).register(awsConnection)
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "RatingsReport")
      val pipelineSpec = PipelineSpec(
         name = "aws-s3-source",
         input = AwsSqsS3TransportInputSpec(
            awsConnection.connectionName,
            RatingReport.versionedType,
            queueName = sqsQueueName,
            pollSchedule = CronExpressions.EVERY_SECOND
         ),
         outputs = listOf(outputSpec)
      )

      startPipeline(hazelcastInstance, vyneClient, pipelineSpec)
      Awaitility.await().atMost(30, TimeUnit.SECONDS).until {
         listSinkTarget.list.isNotEmpty()
      }
   }
}
