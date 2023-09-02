package com.orbitalhq.pipelines.jet

import com.orbitalhq.VyneClient
import com.orbitalhq.VyneClientWithSchema
import com.orbitalhq.VyneProvider
import com.orbitalhq.WebClientVyneQueryService
import com.orbitalhq.embedded.EmbeddedVyneClientWithSchema
import com.orbitalhq.remote.RemoteVyneClientWithSchema
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.spring.config.DiscoveryClientConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.WebClient


/**
 * We currently use two Vyne clients - embedded and remote (if enabled). There are various places where VyneClient is needed such as:
 * - Transformation between source and sink
 * - Taxi operation source & sink (invoke operation)
 * - Validation (accesses schema)
 * - JDBC, S3, Redshift sinks (access schema)
 * - Polling query source (execute a query)
 *
 * The remote client (if enabled) is used for all use cases except for the transformation and Taxi operations.
 *
 * Going forward the transformation phase should also use the remote client, if configured, to offload the transformation to query server.
 */

@Configuration
class EmbeddedVyneConfig(private val vyneProvider: VyneProvider, private val schemaStore: SchemaStore) {

   @Bean
   fun embeddedVyneClient(): VyneClientWithSchema {
      return EmbeddedVyneClientWithSchema(vyneProvider, schemaStore)
   }
}

@Configuration
@Import(DiscoveryClientConfig::class)
@ConditionalOnProperty(
   name = ["vyne.pipelines.query-mode"],
   havingValue = "REMOTE",
   matchIfMissing = false
)
class RemoteVyneConfig(private val schemaStore: SchemaStore) {
   @Bean
   @Primary
   fun remoteVyneClient(webClientBuilder: WebClient.Builder): VyneClient {
      return RemoteVyneClientWithSchema(WebClientVyneQueryService(webClientBuilder), schemaStore)
   }

   @Bean
   @LoadBalanced
   fun loadBalancedWebClientBuilder(): WebClient.Builder {
      return WebClient.builder()
   }
}
