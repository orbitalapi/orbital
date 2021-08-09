package io.vyne

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "vyne.projection")
data class VyneProjectionConfiguration(
    val distributionMode: String = "LOCAL",
    val distributionPacketSize: Int = 50,
    val distributionRemoteBias:Int = 10
)