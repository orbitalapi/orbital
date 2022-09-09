package io.vyne.pipelines.jet

import com.google.common.io.Resources
import io.vyne.VersionedTypeReference
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.connectors.jdbc.builders.PostgresJdbcUrlBuilder
import io.vyne.schemas.Type
import mu.KotlinLogging
import org.awaitility.Awaitility
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.wait.strategy.Wait
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.*

fun <T> queueOf(vararg items: T): Queue<T> {
   return LinkedList(listOf(*items))
}

fun sqsMessageBody(bucket: String, objectKey: String) = """
      {
        "Records": [
          {
            "eventVersion": "2.1",
            "eventSource": "aws:s3",
            "awsRegion": "eu-west-2",
            "eventTime": "2022-02-23T12:46:19.561Z",
            "eventName": "ObjectCreated:Put",
            "userIdentity": {
              "principalId": "AWS:AIDAZNTLYI4JHKHKZBPCA"
            },
            "requestParameters": {
              "sourceIPAddress": "178.251.45.234"
            },
            "responseElements": {
              "x-amz-request-id": "TA893HSC7FW8AVFK",
              "x-amz-id-2": "ZA4SLW5HcGkszaRuhJ5CqUAkn/42E9fABTGS8ixdQfbtaYH3g1mjhDHuFTFtGtMcmETNUw2zOJe9x8Eb8HfoMYRLeUoOasVj"
            },
            "s3": {
              "s3SchemaVersion": "1.0",
              "configurationId": "new-msci-file",
              "bucket": {
                "name": "$bucket",
                "ownerIdentity": {
                  "principalId": "A3DVJL2ZY6JP0O"
                },
                "arn": "arn:aws:s3:::msci-report-csv"
              },
              "object": {
                "key": "$objectKey",
                "size": 39698,
                "eTag": "97e7a7311909726aabc2366de4a84e6f",
                "sequencer": "0062162C9B80307D50"
              }
            }
          }
        ]
      }
   """.trimIndent()

fun populateS3AndSqs(
   localstack: LocalStackContainer,
   bucket: String,
   objectKey: String,
   sqsQueueName: String,
   csvResourceFile: String = "Coinbase_BTCUSD_3rows.csv",
   isLargeUpload: Boolean = false
): String {
   val s3: S3Client = S3Client
      .builder()
      .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
      .region(Region.of(localstack.region))
      .build()
   s3.createBucket { b: CreateBucketRequest.Builder -> b.bucket(bucket) }

   if (isLargeUpload) {
      val TWENTY_MEGABYTE = 1024 * 1024 * 20
      val uploadHelper = MultipartUploadHelper(s3, bucket, objectKey)
      uploadHelper.start()
      val outputStream = ByteArrayOutputStream()
      Resources.getResource(csvResourceFile).openStream().use { fileInputStream ->
         val buffer = ByteArray(TWENTY_MEGABYTE)
         var bytes = fileInputStream.read(buffer)
         while (bytes >= 0) {
            outputStream.write(buffer, 0, bytes)
            bytes = fileInputStream.read(buffer)
            uploadHelper.partUpload(outputStream)
         }
      }
      uploadHelper.complete(outputStream)
   } else {
      s3.putObject(
         { builder -> builder.bucket(bucket).key(objectKey) },
         Paths.get(Resources.getResource(csvResourceFile).path)
      )

   }


   val sqsClient = SqsClient
      .builder()
      .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
      .region(Region.of(localstack.region))
      .build()

   val sqsQueueUrl = sqsClient.createQueue(CreateQueueRequest.builder().queueName(sqsQueueName).build()).queueUrl()
   val sqsMessage =
      SendMessageRequest.builder().messageBody(sqsMessageBody(bucket, objectKey)).queueUrl(sqsQueueUrl).build()
   sqsClient.sendMessage(sqsMessage)
   return sqsQueueUrl
}

private val logger = KotlinLogging.logger { }
fun LocalStackContainer.awsConnection(): AwsConnectionConfiguration {
   return AwsConnectionConfiguration(
      "aws-test-connection",
      this.region,
      this.accessKey,
      this.secretKey,
      this.getEndpointOverride(LocalStackContainer.Service.S3).toString()
   )
}

class PostgresSQLContainerFacade(private val postgreSQLContainer: PostgreSQLContainer<*>) {
   lateinit var database: String
   lateinit var username: String
   lateinit var password: String
   lateinit var host: String
   lateinit var port: String
   lateinit var connection: DefaultJdbcConnectionConfiguration

   fun start() {
      postgreSQLContainer.start()
      postgreSQLContainer.waitingFor(Wait.forListeningPort())

      port = postgreSQLContainer.firstMappedPort.toString()
      username = postgreSQLContainer.username
      password = postgreSQLContainer.password
      database = postgreSQLContainer.databaseName
      host = postgreSQLContainer.host

      connection = DefaultJdbcConnectionConfiguration.forParams(
         "test-connection",
         JdbcDriver.POSTGRES,
         connectionParameters = mapOf(
            PostgresJdbcUrlBuilder.Parameters.HOST to host,
            PostgresJdbcUrlBuilder.Parameters.PORT to port,
            PostgresJdbcUrlBuilder.Parameters.DATABASE to database,
            PostgresJdbcUrlBuilder.Parameters.USERNAME to username,
            PostgresJdbcUrlBuilder.Parameters.PASSWORD to password,
         )
      )
   }

   fun waitForRowCount(
      dsl: DSLContext,
      type: Type,
      rowCount: Int,
      startTime: Instant = Instant.now(),
      duration: Duration = Duration.ofSeconds(30)
   ) {
      Awaitility.await().atMost(duration)
         .until {
            val currentRowCount = rowCount(dsl, type)
            logger.info(
               "Row count after ${
                  Duration.between(startTime, Instant.now()).toMillis()
               }ms is $currentRowCount (Waiting until it hits $rowCount)"
            )
            currentRowCount >= rowCount
         }
   }

   private fun rowCount(dsl: DSLContext, type: Type): Int {
      return try {
         dsl.fetchCount(
            DSL.table(SqlUtils.tableNameOrTypeName(type.taxiType))
         )
      } catch (e: Exception) {
         -1
      } // return -1 if the table doesn't exist
   }

}

object RatingReport {
   const val typeName = "RatingsReport"
   val versionedType = VersionedTypeReference.parse(typeName)
   fun ratingsSchema(formatSpecification: String = "") = """
         type IssuerName inherits String
         type IssuerId inherits String
         type Isin inherits String
         type IssuerCountryOfDomicile inherits String
         type EffectiveDate inherits Date
         enum Rating {
             A,
             AA,
             AAA,
             B,
             BB,
             BBB
         }
         enum IvaCompanyRating inherits Rating
         type IndustryAdjustedScore inherits Decimal
         type IvaRatingDate inherits Date(@format="yyyyMMdd")
         type IvaIndustry inherits String
         enum IvaPreviousRating inherits Rating
         type NumericTrend inherits Int
         type IvaRatingTrend inherits NumericTrend
         type WeightedAverageScore inherits Decimal
         type EsgPillarScore inherits Decimal
         type EsgPillarWeight inherits Int
         type EnvironmentalPillarScore inherits EsgPillarScore
         type EnvironmentalPillarWeight inherits EsgPillarWeight
         type SocialPillarScore inherits EsgPillarScore
         type SocialPillarWeight inherits EsgPillarWeight
         type GovernancePillarScore inherits EsgPillarScore
         type GovernancePillarWeight inherits EsgPillarWeight
         type CarbonIntensityWeightedAverageByRevenue inherits Decimal

         $formatSpecification
         model $typeName {
             issuerName : IssuerName by column(1)
             issuerId : IssuerId by column(2)
             isin : Isin by column(18)
             issuerCountryOfDomicile : IssuerCountryOfDomicile by column(3)
             effectiveDate : EffectiveDate(@format = "yyyyMMdd") by column(4)
             ivaCompanyRating : IvaCompanyRating by column(5)
             industryAdjustedScore : IndustryAdjustedScore by column(6)
             ivaRatingDate : IvaRatingDate by column(7)
             ivaIndustry : IvaIndustry by column(8)
             ivaPreviousRating : IvaPreviousRating? by column(9)
             ivaRatingTrend : IvaRatingTrend? by column(10)
             weightedAverageScore : WeightedAverageScore by column(11)
             environmentalPillarScore : EnvironmentalPillarScore by column(12)
             environmentalPillarWeight : EnvironmentalPillarWeight by column(13)
             socialPillarScore : SocialPillarScore by column(14)
             socialPillarWeight : SocialPillarWeight by column(15)
             governancePillarScore : GovernancePillarScore by column(16)
             governancePillarWeight : GovernancePillarWeight by column(17)
             carbonIntensityWeightedAverage : CarbonIntensityWeightedAverageByRevenue by column(19)
         }""".trimIndent()
}

