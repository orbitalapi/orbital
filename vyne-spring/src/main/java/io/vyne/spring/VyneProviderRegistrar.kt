package io.vyne.spring

import io.micrometer.core.instrument.MeterRegistry
import io.vyne.VyneCacheConfiguration
import io.vyne.query.connectors.OperationInvoker
import io.vyne.query.connectors.batch.OperationBatchingStrategy
import io.vyne.schema.consumer.SchemaStore
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.DefaultRequestFactory
import io.vyne.spring.http.auth.AuthTokenInjectingRequestFactory
import io.vyne.spring.http.auth.AuthTokenRepository
import io.vyne.spring.invokers.AbsoluteUrlResolver
import io.vyne.spring.invokers.RestTemplateInvoker
import io.vyne.spring.invokers.ServiceDiscoveryClientUrlResolver
import io.vyne.spring.invokers.ServiceUrlResolver
import io.vyne.spring.invokers.SpringServiceDiscoveryClient
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
class EnableVyneConfiguration {
   @Bean
   fun vyneFactory(
       schemaStore: SchemaStore,
       operationInvokers: List<OperationInvoker>,
       operationBatchingStrategies: List<OperationBatchingStrategy>,
       vyneCacheConfiguration: VyneCacheConfiguration,
       vyneSpringProjectionConfiguration: VyneSpringProjectionConfiguration
   ): VyneFactory {
      return VyneFactory(schemaStore, operationInvokers, operationBatchingStrategies, vyneCacheConfiguration, vyneSpringProjectionConfiguration)
   }


   @Bean
   fun restTemplateOperationInvoker(
       schemaStore: SchemaStore,
       webClientBuilder: WebClient.Builder,
       serviceUrlResolvers: List<ServiceUrlResolver>,
       authTokenRepository: AuthTokenRepository,
       meterRegistry: MeterRegistry
   ): RestTemplateInvoker {
      val requestFactory = AuthTokenInjectingRequestFactory(
         DefaultRequestFactory(),
         authTokenRepository
      )
      return RestTemplateInvoker(schemaStore, webClientBuilder, serviceUrlResolvers, requestFactory)
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
