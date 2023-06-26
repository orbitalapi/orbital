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
import org.springframework.cloud.client.ServiceInstance
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
               getRsocketPort(serviceInstance, config)
            }
         } else {
            val uri = URI.create(config.schemaServerAddress)
            AddressSupplier.Companion.just(TcpAddress(uri.host, config.schemaServerRSocketPort))
         } as AddressSupplier<ClientTransportAddress>
      return SchemaServerRSocketFactory(addressSupplier)
   }

   private fun getRsocketPort(
      serviceInstance: ServiceInstance,
      config: SchemaConfigProperties
   ): Int {
      // In the old days, we used to support just declaring the rsocket port.
      // It wasn't obvious to people what it meant, so we're now asking people to declare the full
      // uri.
      // If they're using the old version, that's fine.
      val legacyDeclaredRsocketPort = serviceInstance.metadata["rsocket-port"]?.let { rsocketPort ->
         val rsocketPortInt = rsocketPort.toIntOrNull()
         if (rsocketPortInt != null) {
            logger.warn { "Could not parse the provided rsocket-port value ('$rsocketPort') to an int.  Using the fallback value of ${config.schemaServerRSocketPort}" }
         }
         rsocketPortInt ?: config.schemaServerRSocketPort
      }
      if (legacyDeclaredRsocketPort != null) {
         logger.warn { "Defining the rsocket connection using rsocket-port is deprecated.  Instead declare use a URI.  Your config equivalent is a config entry of 'rsocket' within the schema-server config block with a value of tcp://${serviceInstance.host}:$legacyDeclaredRsocketPort" }
         return legacyDeclaredRsocketPort
      }

      // Try reading the newer rsocket URI declaration
      val rsocketMetadata = serviceInstance.metadata["rsocket"] ?: return config.schemaServerRSocketPort

      // Parse the configured rsocket entry if present.
      return try {
         val uri = URI.create(rsocketMetadata)
         uri.port
      } catch (e: Exception) {
         logger.warn { "Failed to parse a URI from schema server rsocket value of $rsocketMetadata.  Expected a uri like tcp://schema-server.com:7655. Falling back to configured value of ${config.schemaServerRSocketPort}" }
         config.schemaServerRSocketPort
      }


   }


}
