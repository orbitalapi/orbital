package io.vyne.pipelines.jet.integration

import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.PostgresSQLContainerFacade
import io.vyne.pipelines.jet.RatingReport
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import io.vyne.pipelines.jet.awsConnection
import io.vyne.pipelines.jet.populateS3AndSns
import io.vyne.pipelines.jet.source.aws.sqss3.SqsS3SourceBuilder
import org.awaitility.Awaitility
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.concurrent.TimeUnit

@Testcontainers
@RunWith(SpringRunner::class)
class SqsS3SourceJdbcSinkIntegrationTest : BaseJetIntegrationTest() {
   private val localStackImage: DockerImageName = DockerImageName.parse("localstack/localstack").withTag("0.14.0")
   private var sqsQueueUrl = ""

   lateinit var postgresSQLContainerFacade: PostgresSQLContainerFacade

   @Rule
   @JvmField
   val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1") as PostgreSQLContainer<*>

   @JvmField
   @Rule
   var localstack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.SQS)

   @Before
   fun setUp() {
      sqsQueueUrl = populateS3AndSns(
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
      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
         RatingReport.ratingsSchema("@io.vyne.formats.Csv"),
         listOf(postgresSQLContainerFacade.connection),
         listOf(localstack.awsConnection())
      )
      // Register the connection so we can look it up later
      val connectionRegistry = applicationContext.getBean(JdbcConnectionRegistry::class.java)
      connectionRegistry.register(postgresSQLContainerFacade.connection)
      applicationContext.getBean(AwsConnectionRegistry::class.java).register(localstack.awsConnection())
      val vyne = vyneProvider.createVyne()

      // create the pipeline
      val pipelineSpec = PipelineSpec(
         name = "snss3-to-jdbc-pipeline",
         input = AwsSqsS3TransportInputSpec(
            localstack.awsConnection().connectionName,
            RatingReport.versionedType,
            queueName = sqsQueueUrl,
            pollSchedule = CronExpressions.EVERY_SECOND,
            endPointOverride = localstack.getEndpointOverride(LocalStackContainer.Service.S3)
         ),
         output = JdbcTransportOutputSpec(
            "test-connection",
            RatingReport.typeName
         )
      )

      // start the pipeline.
      val (_, job) = startPipeline(jetInstance, vyneProvider, pipelineSpec)
      val connectionFactory = applicationContext.getBean(JdbcConnectionFactory::class.java)
      val type = vyne.type(RatingReport.typeName)


      postgresSQLContainerFacade.waitForRowCount(
         connectionFactory.dsl(postgresSQLContainerFacade.connection),
         type,
         281
      )
   }


}
