package io.vyne.pipelines.jet.sink.aws.s3

import com.hazelcast.jet.pipeline.test.TestSources
import com.winterbe.expekt.should
import io.vyne.connectors.aws.core.AwsConnection
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportOutputSpec
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.pipelines.jet.source.fixed.ItemStreamSourceSpec
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
import software.amazon.awssdk.services.s3.model.S3Object
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class AwsS3SinkTest : BaseJetIntegrationTest() {
   val localStackImage: DockerImageName = DockerImageName.parse("localstack/localstack").withTag("0.14.0")
   val bucket = "testbucket"
   val objectKey = "example.csv"

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
   fun `can submit a single item to AWS S3`() {
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
            nullValue = "NULL",
            useFieldNamesAsColumnNames = true
         )
         model Target {
            givenName : FirstName
            surname : LastName
         }
      """, awsConnections = listOf(awsConnectionConfig)
      )

      val pipelineSpec = PipelineSpec(
         "test-aws-s3-sink",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "Jimmy", "lastName" : "Schmitt" }"""),
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
      contents.trimEnd().should.equal("""Jimmy|Schmitt""")
   }

   @Test
   fun `can submit a list to AWS S3`() {
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
            nullValue = "NULL",
            useFieldNamesAsColumnNames = true
         )
         model Target {
            givenName : FirstName
            surname : LastName
         }
      """, awsConnections = listOf(awsConnectionConfig)
      )

      val pipelineSpec = PipelineSpec(
         "test-aws-s3-sink",
         input = FixedItemsSourceSpec(
            items = queueOf("""[{ "firstName" : "Jimmy", "lastName" : "Schmitt" }, { "firstName" : "Jimmy2", "lastName" : "Schmitt2" }]"""),
            typeName = "Person".fqn()
         ),
         output = AwsS3TransportOutputSpec(
            "test-aws",
            bucket,
            objectKey,
            "Target[]"
         )
      )

      startPipeline(jetInstance, vyneProvider, pipelineSpec)
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         s3.listObjectsV2 { it.bucket(bucket) }.contents().any { it.key() == objectKey }
      }
      val contents = IOUtils.toString(s3.getObject { it.bucket(bucket).key(objectKey) }, StandardCharsets.UTF_8)
      contents.trimEnd().should.equal(
         """givenName|surname
         |Jimmy|Schmitt
         |Jimmy2|Schmitt2
      """.trimMargin()
      )
   }

   @Test
   fun `allows specifying a timestamp replacement in the filename`() {
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
            nullValue = "NULL",
            useFieldNamesAsColumnNames = true
         )
         model Target {
            givenName : FirstName
            surname : LastName
         }
      """, awsConnections = listOf(awsConnectionConfig)
      )

      val pipelineSpec = PipelineSpec(
         "test-aws-s3-sink",
         input = FixedItemsSourceSpec(
            items = queueOf("""[{ "firstName" : "Jimmy", "lastName" : "Schmitt" }, { "firstName" : "Jimmy2", "lastName" : "Schmitt2" }]"""),
            typeName = "Person".fqn()
         ),
         output = AwsS3TransportOutputSpec(
            "test-aws",
            bucket,
            "example-{env.now}.csv",
            "Target[]"
         )
      )

      startPipeline(jetInstance, vyneProvider, pipelineSpec)
      var file: S3Object? = null
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         file = s3.listObjectsV2 { it.bucket(bucket) }.contents()
            .find { it.key().startsWith("example-") && it.key().endsWith(".csv") && !it.key().contains("{env.now}") }
         file != null
      }
      val contents = IOUtils.toString(s3.getObject { it.bucket(bucket).key(file!!.key()) }, StandardCharsets.UTF_8)
      contents.trimEnd().should.equal(
         """givenName|surname
         |Jimmy|Schmitt
         |Jimmy2|Schmitt2
      """.trimMargin()
      )
   }

   @Test
   fun `can handle very big amounts of data`() {
      awsConnectionRegistry.register(awsConnectionConfig)

      val (jetInstance, _, vyneProvider) = jetWithSpringAndVyne(
         """
         model Person {
            @Id
            id : PersonId inherits Int by column(1)
            firstName : FirstName inherits String by column(2)
            lastName : LastName inherits String by column(3)
         }

         @io.vyne.formats.Csv(
            delimiter = "|",
            nullValue = "NULL",
            useFieldNamesAsColumnNames = true
         )
         model Target {
            id : PersonId by default("")
            givenName : FirstName by default("")
            surname : LastName by default("")
         }
      """, awsConnections = listOf(awsConnectionConfig)
      )

      val stream = TestSources.itemStream(1000) { timestamp: Long, sequence: Long ->
         StringContentProvider("$sequence,Jimmy $sequence,Smitts")
      }
      val pipelineSpec = PipelineSpec(
         "test-aws-s3-sink",
         input = ItemStreamSourceSpec(
            source = stream,
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
      Awaitility.await().atMost(30, TimeUnit.SECONDS).until {
         s3.listObjectsV2 { it.bucket(bucket) }.contents().any { it.key() == objectKey }
      }
      val contents = IOUtils.toString(s3.getObject { it.bucket(bucket).key(objectKey) }, StandardCharsets.UTF_8)
      contents.trimEnd().should.equal(
         """givenName|surname
         |Jimmy|Schmitt
         |Jimmy2|Schmitt2
      """.trimMargin()
      )
   }
}
