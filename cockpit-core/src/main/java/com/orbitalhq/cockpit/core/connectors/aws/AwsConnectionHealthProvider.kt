package com.orbitalhq.cockpit.core.connectors.aws

import arrow.core.getOrElse
import com.orbitalhq.cockpit.core.connectors.ConnectionHealthProvider
import com.orbitalhq.connections.ConnectionStatus
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorCategory
import io.vyne.connectors.aws.core.registry.test
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AwsConnectionHealthProvider: ConnectionHealthProvider {
    override fun canProvideFor(config: ConnectorConfiguration): Boolean  = config.type == ConnectorCategory.AWS

    override fun provide(config: ConnectorConfiguration): Mono<ConnectionStatus> {
        require(config is AwsConnectionConfiguration) { "Expected to receive a ${AwsConnectionConfiguration::class.simpleName}, but got ${config::class.simpleName}"}
        return Mono.create { sink ->
            val result = try {
                config.test()
                    .map { ConnectionStatus.healthy() }
                    .getOrElse { ConnectionStatus.error(it) }
            } catch (e:Exception) {
                ConnectionStatus.error(e.message ?: e::class.simpleName!!)
            }
            sink.success(result)
        }
    }
}
