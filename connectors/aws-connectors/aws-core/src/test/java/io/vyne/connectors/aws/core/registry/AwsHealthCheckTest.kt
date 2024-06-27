package io.vyne.connectors.aws.core.registry

import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.winterbe.expekt.should
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class AwsHealthCheckTest {
    private val localStackImage = DockerImageName.parse("localstack/localstack").withTag("3.0")

    @Container
    var localStack: LocalStackContainer = LocalStackContainer(localStackImage)
        .withServices(LocalStackContainer.Service.STS)

    @Test
    fun canPerformAwsHealthCheck() {
        val connectionConfig = AwsConnectionConfiguration(
            connectionName = "vyneAws",
            accessKey = localStack.accessKey,
            secretKey = localStack.secretKey,
            region = localStack.region,
            endPointOverride = localStack.getEndpointOverride(
                LocalStackContainer.Service.DYNAMODB
            ).toString()
        )

        connectionConfig.test().isRight().should.equal(true)

    }
}