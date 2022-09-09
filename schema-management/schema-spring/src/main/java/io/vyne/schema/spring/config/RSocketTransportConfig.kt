package io.vyne.schema.spring.config

import io.vyne.schema.api.AddressSupplier
import io.vyne.schema.rsocket.ClientTransportAddress
import io.vyne.schema.rsocket.SchemaServerRSocketFactory
import io.vyne.schema.rsocket.TcpAddress
import io.vyne.schema.spring.RSocketHealthIndicator
import io.vyne.schema.spring.config.SchemaConfigProperties.*
import io.vyne.schema.spring.config.consumer.SchemaConsumerConfigProperties
import io.vyne.schema.spring.config.publisher.SchemaPublisherConfigProperties.Companion.PUBLISHER_METHOD
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
@ConditionalOnProperty(
   value = [
      SchemaConsumerConfigProperties.CONSUMER_METHOD,
      PUBLISHER_METHOD
   ],
   havingValue = "RSocket",
   matchIfMissing = true
)
class RSocketTransportConfig {
   private val logger = KotlinLogging.logger {}

   // Note: Don't rename this bean.
   // Spring Boot uses the bean name to infer the schema health endpoint by convention
   // rsocketHealthIndicator is available at /api/actuator/health/rsocket
   @Bean
   fun rsocketHealthIndicator(factory: SchemaServerRSocketFactory): RSocketHealthIndicator {
      return RSocketHealthIndicator(factory)
   }

   /**
    * Builds an RSocket factory configured with an AddressSupplier that
    * either uses a discovery client (if available) or simply the configured
    * host and port
    */
   @Bean
   fun schemaServerRSocketFactory(
      discoveryClient: DiscoveryClient?,
      config: SchemaConfigProperties,
   ): SchemaServerRSocketFactory {
      val addressSupplier =
         if (discoveryClient != null && config.schemaServerAddressType == AddressType.DISCOVERY_CLIENT_REFERENCE) {
            logger.info { "SchemaServer rsocket connections will be made via DiscoveryClient of type ${discoveryClient::class.simpleName}" }
            // Configure RSocket lookup via discovery client
            DiscoveryClientAddressSupplier.forTcpAddresses(
               discoveryClient,
               config.schemaServerAddress
            ) { serviceInstance ->
               serviceInstance.metadata["rsocket-port"]?.toIntOrNull() ?: config.schemaServerRSocketPort
            }
         } else {
            val uri = URI.create(config.schemaServerAddress)
            AddressSupplier.Companion.just(TcpAddress(uri.host, config.schemaServerRSocketPort))
         } as AddressSupplier<ClientTransportAddress>
      return SchemaServerRSocketFactory(addressSupplier)
   }


}
