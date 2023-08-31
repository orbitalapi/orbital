package io.vyne.pipelines.jet.integration

import io.vyne.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.pipelines.jet.*
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Disabled
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Ignore("Flakey - breaking the build")
@Disabled("Flakey - breaking the build")
@Testcontainers
@RunWith(SpringRunner::class)
class SqsS3SourceJdbcSinkIntegrationTest : BaseJetIntegrationTest() {
   private val localStackImage: DockerImageName = DockerImageName.parse("localstack/localstack").withTag("1.0.4")
   private lateinit var sqsQueueUrl: String

   private lateinit var postgresSQLContainerFacade: PostgresSQLContainerFacade

   @Rule
   @JvmField
   val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1") as PostgreSQLContainer<*>

   @JvmField
   @Rule
   var localstack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.SQS)

   @Before
   fun setUp() {
      sqsQueueUrl = populateS3AndSqs(
         localstack,
         "ratings-port-bucket",
         "ratings-report.csv",
         "ratings-report-queue",
         "msci-stub.csv"
      )
      postgresSQLContainerFacade = PostgresSQLContainerFacade(postgreSQLContainer)
      postgresSQLContainerFacade.start()
   }

   @Test
   fun `s3sqs source and jdbc postgres sink`() {
      val testSetup = jetWithSpringAndVyne(
         RatingReport.ratingsSchema("@io.vyne.formats.Csv"),
         listOf(postgresSQLContainerFacade.connection),
         listOf(localstack.awsConnection())
      )
      // Register the connection so we can look it up later
      val connectionRegistry = testSetup.applicationContext.getBean(InMemoryJdbcConnectionRegistry::class.java)
      connectionRegistry.register(postgresSQLContainerFacade.connection)
      testSetup.applicationContext.getBean(AwsInMemoryConnectionRegistry::class.java)
         .register(localstack.awsConnection())

      // create the pipeline
      val pipelineSpec = PipelineSpec(
         name = "sqss3-to-jdbc-pipeline",
         input = AwsSqsS3TransportInputSpec(
            localstack.awsConnection().connectionName,
            RatingReport.versionedType,
            queueName = sqsQueueUrl,
            pollSchedule = CronExpressions.EVERY_SECOND
         ),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               RatingReport.typeName
            )
         )
      )

      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)
      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)
      val type = testSetup.schema.type(RatingReport.typeName)

      postgresSQLContainerFacade.waitForRowCount(
         connectionFactory.dsl(postgresSQLContainerFacade.connection),
         type,
         279 // The file contains 281 rows out of which 2 have missing values for mandatory fields
      )
   }


}
