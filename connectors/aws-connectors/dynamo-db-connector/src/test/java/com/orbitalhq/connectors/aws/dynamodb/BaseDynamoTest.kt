package com.orbitalhq.connectors.aws.dynamodb

import com.orbitalhq.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.math.BigDecimal

@Testcontainers
abstract class BaseDynamoTest {
   private val localStackImage = DockerImageName.parse("localstack/localstack").withTag("3.0")

   @Container
   var localStack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.DYNAMODB)

   protected val connectionRegistry = AwsInMemoryConnectionRegistry()

   @BeforeEach
   fun beforeTest() {
      val connectionConfig = AwsConnectionConfiguration(
         connectionName = "vyneAws",
         accessKey = localStack.accessKey,
         secretKey = localStack.secretKey,
         region = localStack.region,
         endPointOverride = localStack.getEndpointOverride(
            LocalStackContainer.Service.DYNAMODB
         ).toString()
      )
      connectionRegistry.register(connectionConfig)
   }


   protected fun buildClient(): DynamoDbClient = DynamoDbClient.builder()
      .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
      .region(Region.of(localStack.region))
      .credentialsProvider(
         StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
               localStack.accessKey,
               localStack.secretKey
            )
         )
      )
      .build()

}


fun String.asAttribute(): AttributeValue = AttributeValue.fromS(this)
fun Int.asAttribute() = AttributeValue.fromN(this.toString())
fun List<String>.asAttribute(): AttributeValue = AttributeValue.fromSs(this)
fun List<BigDecimal>.asNsAttribute(): AttributeValue = AttributeValue.fromNs(this.map { it.toString() })
fun BigDecimal.asAttribute():AttributeValue = AttributeValue.fromN(this.toString())
