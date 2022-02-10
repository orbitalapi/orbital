package io.vyne.spring

import io.micrometer.core.instrument.MeterRegistry
import io.vyne.VyneCacheConfiguration
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemaConsumerApi.SchemaStore
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
      vyneCacheConfiguration: VyneCacheConfiguration,
      vyneSpringProjectionConfiguration: VyneSpringProjectionConfiguration
   ): VyneFactory {
      return VyneFactory(schemaStore, operationInvokers, vyneCacheConfiguration, vyneSpringProjectionConfiguration)
   }


   /**
    * Only used if a Hazlecast instance hasn't already been provided.
    * Note that if @VyneSchemaPublisher or @VyneSchemaConsumer is enabled, and the schema distribution
    * is set to DISTRIBUTED (either via annotation, or via property source) then
    * a hazelcast instance is wired in via VyneSchemaStoreConfigRegistrar.
    *
    * Using  @VyneSchemaPublisher or @VyneSchemaConsumer is preferrable, as the VYNE_SCHEMA_PUBLICATION_METHOD
    * is considered both from annotations and property sources.  In the below, only properties are considered.
    */
   // TODO : Why do we need this?  Don't we create one in VyneSchemaPublisher and VyneSchemaConsumer?
//   @Bean("hazelcast")
//   @ConditionalOnMissingBean(HazelcastInstance::class)
//   @ConditionalOnProperty(VYNE_SCHEMA_PUBLICATION_METHOD, havingValue = "DISTRIBUTED")
//   fun defaultHazelCastInstance(): HazelcastInstance {
//      return Hazelcast.newHazelcastInstance()
//   }

   // TODO : This can't be left like this, as it would effect other rest templates within
   // the target application.
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
