package io.vyne.spring.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.projection")
data class VyneSpringProjectionConfiguration(
    val distributionMode: ProjectionDistribution = ProjectionDistribution.LOCAL,
    val distributionPacketSize: Int = 100,
    val distributionRemoteBias: Int = 10
)

enum class ProjectionDistribution {
    LOCAL,
    HAZELCAST
}
