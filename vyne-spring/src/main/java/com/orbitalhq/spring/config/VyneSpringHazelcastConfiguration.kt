package com.orbitalhq.spring.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "vyne.hazelcast")
data class VyneSpringHazelcastConfiguration(
    val discovery: HazelcastDiscovery = HazelcastDiscovery.MULTICAST,
    val memberTag: String = "vyne-worker",
    val eurekaUri: String = "http://eureka",
    val networkInterface: String = "",
    val useMetadataForHostAndPort: String = "false",
    val awsPortScanRange: String = "5701-5751",
    val taskPoolSize: Int = 2,
    val taskQueueSize: Int = 0
)

enum class HazelcastDiscovery {
    MULTICAST,
    AWS,
}