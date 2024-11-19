package com.orbitalhq.cockpit.core.connectors.aws

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.winterbe.expekt.should
import io.vyne.connectors.aws.core.registry.test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.kotlin.test.test
import java.time.Duration

@Testcontainers
class AwsControllerTest {
   private val localStackImage = DockerImageName.parse("localstack/localstack").withTag("3.0")

   @Container
   val localStack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.STS)

   @Test
   fun `testing healthy connection returns ok `() {
      val controller = AwsController(mock { })
      val connectionConfig = AwsConnectionConfiguration(
         connectionName = "vyneAws",
         accessKey = localStack.accessKey,
         secretKey = localStack.secretKey,
         region = localStack.region,
         endPointOverride = localStack.getEndpointOverride(
            LocalStackContainer.Service.DYNAMODB
         ).toString()
      )

      controller.testConnection(connectionConfig)
         .test()
         .expectSubscription()
         .expectNextMatches { connectionStatus -> connectionStatus.status == ConnectionStatus.Status.OK }
         .expectComplete()
         .verify(Duration.ofSeconds(5))
   }
}
