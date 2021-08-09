package io.vyne.spring

import io.vyne.VyneCacheConfiguration
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.spring.invokers.*
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(EnableVyneConfiguration::class)
annotation class EnableVyne

@Configuration
class EnableVyneConfiguration  {
   @Bean
   fun vyneFactory(schemaProvider: SchemaSourceProvider, operationInvokers: List<OperationInvoker>, vyneCacheConfiguration: VyneCacheConfiguration): VyneFactory {
      return VyneFactory(schemaProvider, operationInvokers, vyneCacheConfiguration)
   }

   // TODO : This can't be left like this, as it would effect other rest templates within
   // the target application.
   @Bean
   fun restTemplateOperationInvoker(schemaProvider: SchemaProvider,
                                    webClientBuilder: WebClient.Builder,
                                    serviceUrlResolvers: List<ServiceUrlResolver>
   ): RestTemplateInvoker {
      return RestTemplateInvoker(schemaProvider, webClientBuilder, serviceUrlResolvers)
   }

   @Bean
   fun serviceDiscoveryUrlResolver(discoveryClient: DiscoveryClient): ServiceDiscoveryClientUrlResolver {
      return ServiceDiscoveryClientUrlResolver(SpringServiceDiscoveryClient(discoveryClient))
   }

   @Bean
   fun absoluteUrlResolver(): AbsoluteUrlResolver {
      return AbsoluteUrlResolver()
   }

}
