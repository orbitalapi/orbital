package io.vyne.spring.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.projection")
data class VyneSpringProjectionConfiguration(
    val distributionMode: String,
    val distributionPacketSize: Long,
    val distributionRemoteBias: Long
)