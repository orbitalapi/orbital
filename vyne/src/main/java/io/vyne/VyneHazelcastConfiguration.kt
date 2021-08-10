package io.vyne

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.hazelcast")
data class VyneHazelcastConfiguration(
    val enabled: Boolean,
    val discovery: String,
    val memberTag: String
)
