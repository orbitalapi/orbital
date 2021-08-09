package io.vyne

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.projection")
data class VyneProjectionConfiguration(
    val distributionMode: String,
    val distributionPacketSize: Long,
    val distributionRemoteBias: Long
)