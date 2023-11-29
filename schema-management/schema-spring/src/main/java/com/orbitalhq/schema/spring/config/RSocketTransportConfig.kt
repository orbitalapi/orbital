package com.orbitalhq.schema.spring.config

import com.orbitalhq.http.ServicesConfig
import com.orbitalhq.http.ServicesConfig.Companion.ORBITAL_SERVER_NAME
import com.orbitalhq.schema.api.AddressSupplier
import com.orbitalhq.schema.rsocket.ClientTransportAddress
import com.orbitalhq.schema.rsocket.SchemaServerRSocketFactory
import com.orbitalhq.schema.spring.RSocketHealthIndicator
import com.orbitalhq.schema.spring.config.consumer.SchemaConsumerConfigProperties
import com.orbitalhq.schema.spring.config.publisher.SchemaPublisherConfigProperties.Companion.PUBLISHER_METHOD
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
      discoveryClient: DiscoveryClient,
   ): SchemaServerRSocketFactory {
      val addressSupplier = DiscoveryClientAddressSupplier.forTcpAddresses(
         discoveryClient,
         ORBITAL_SERVER_NAME
      ) { serviceInstance ->
         getRsocketPort(serviceInstance)
      } as AddressSupplier<ClientTransportAddress>
      return SchemaServerRSocketFactory(addressSupplier)
   }

   private fun getRsocketPort(
      serviceInstance: ServiceInstance,
   ): Int {
      // Try reading the newer rsocket URI declaration
      val port =  serviceInstance.metadata["rsocket"]?.let {rsocketMetadata ->
         try {
            val uri = URI.create(rsocketMetadata)
            uri.port
         } catch (e: Exception) {
            logger.warn { "Failed to parse a URI from schema server rsocket value of $rsocketMetadata.  Expected a uri like tcp://schema-server.com:7655. Falling back to default port of ${ServicesConfig.DEFAULT_QUERY_SERVER_RSOCKET_PORT}" }
            ServicesConfig.DEFAULT_QUERY_SERVER_RSOCKET_PORT
         }
      } ?: return ServicesConfig.DEFAULT_QUERY_SERVER_RSOCKET_PORT

      return port


   }


}
