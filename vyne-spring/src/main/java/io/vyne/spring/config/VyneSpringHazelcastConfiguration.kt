package io.vyne.spring.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.hazelcast")
data class VyneSpringHazelcastConfiguration(
    val enabled: Boolean,
    val discovery: String,
    val memberTag: String,
    val eurekaUri: String,
    val networkInterface: String = ""
)
