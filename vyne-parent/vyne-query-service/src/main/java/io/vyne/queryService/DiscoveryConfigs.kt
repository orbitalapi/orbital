package io.vyne.queryService

import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource

@Profile("embedded-discovery")
@EnableEurekaServer
@Configuration
class EmbeddedDiscoveryServerConfig

@Profile("!embedded-discovery")
@EnableEurekaClient
@Configuration
class ExternalDiscoveryServerConfig
