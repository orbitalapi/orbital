package io.vyne.spring

import io.micrometer.core.instrument.MeterRegistry
import io.vyne.VyneCacheConfiguration
import io.vyne.auth.tokens.AuthTokenRepository
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.consumer.SchemaStore
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.DefaultRequestFactory
import io.vyne.spring.http.auth.AuthTokenInjectingRequestFactory
import io.vyne.spring.invokers.RestTemplateInvoker
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

   @Bean
   fun restTemplateOperationInvoker(
      schemaProvider: SchemaProvider,
      webClientBuilder: WebClient.Builder,
      authTokenRepository: AuthTokenRepository,
      meterRegistry: MeterRegistry,
   ): RestTemplateInvoker {
      val requestFactory = AuthTokenInjectingRequestFactory(
         DefaultRequestFactory(),
         authTokenRepository
      )
      return RestTemplateInvoker(schemaProvider, webClientBuilder,  requestFactory)
   }

}
