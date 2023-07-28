package io.vyne.spring

import io.micrometer.core.instrument.MeterRegistry
import io.vyne.VyneCacheConfiguration
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.auth.schemes.AuthWebClientCustomizer
import io.vyne.spring.invokers.RestTemplateInvoker
import io.vyne.spring.query.formats.FormatSpecRegistry
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
      schemaProvider: SchemaProvider,
      operationInvokers: List<OperationInvoker>,
      vyneCacheConfiguration: VyneCacheConfiguration,
      vyneSpringProjectionConfiguration: VyneSpringProjectionConfiguration,
      formatSpecRegistry: FormatSpecRegistry
   ): VyneFactory {
      return VyneFactory(schemaProvider, operationInvokers, vyneCacheConfiguration, vyneSpringProjectionConfiguration, formatSpecRegistry = formatSpecRegistry)
   }

   @Bean
   fun restTemplateOperationInvoker(
      schemaProvider: SchemaProvider,
      webClientBuilder: WebClient.Builder,
      authWebClientCustomizer: AuthWebClientCustomizer,
      meterRegistry: MeterRegistry,
   ): RestTemplateInvoker {
//      val requestFactory = AuthTokenInjectingRequestFactory(
//         DefaultRequestFactory(),
//         authTokenRepository
//      )
      return RestTemplateInvoker(schemaProvider, webClientBuilder, authWebClientCustomizer)
   }

}
