package com.orbitalhq.nebula

import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.spring.config.LoadBalancerFilterFunction
import com.orbitalhq.spring.invokers.WebClientFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient

@Configuration
@ConditionalOnProperty("vyne.toggles.nebulaEnabled", matchIfMissing = false, havingValue = "true")
@ComponentScan
class NebulaSpringModule {

   @Bean
   fun nebulaWatcher(schemaEventSource: SchemaChangedEventProvider): NebulaSchemaWatcher {
      return NebulaSchemaWatcher.fromEventSource(schemaEventSource)
   }

   @Bean
   fun nebulaApi(webClientBuilder: WebClient.Builder, discoveryClient: DiscoveryClient): NebulaApi {
      val webClient = webClientBuilder
         .filter(LoadBalancerFilterFunction(discoveryClient))
         .build()
      val adapter = WebClientAdapter.create(webClient)
      val serviceFactory = HttpServiceProxyFactory.builderFor(adapter).build()
      val nebulaApi: NebulaApi = serviceFactory.createClient()
      return nebulaApi
   }
}
