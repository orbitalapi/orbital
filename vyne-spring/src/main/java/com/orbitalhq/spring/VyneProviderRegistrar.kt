package com.orbitalhq.spring

import io.micrometer.core.instrument.MeterRegistry
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.query.connectors.OperationCacheProviderBuilder
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.graph.operationInvocation.cache.OperationCacheFactory
import com.orbitalhq.query.graph.operationInvocation.cache.local.LocalCacheProviderBuilder
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.spring.config.VyneSpringProjectionConfiguration
import com.orbitalhq.spring.http.auth.schemes.AuthWebClientCustomizer
import com.orbitalhq.spring.invokers.RestTemplateInvoker
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import lang.taxi.packages.utils.log
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(EnableVyneConfiguration::class)
annotation class EnableVyne

@Configuration
@Import(FormatSpecRegistry::class)
class EnableVyneConfiguration {
   @Bean
   fun vyneFactory(
      schemaProvider: SchemaProvider,
      operationInvokers: List<OperationInvoker>,
      vyneCacheConfiguration: VyneCacheConfiguration,
      vyneSpringProjectionConfiguration: VyneSpringProjectionConfiguration,
      formatSpecRegistry: FormatSpecRegistry,
      operationCacheFactory: OperationCacheFactory
   ): VyneFactory {
      return VyneFactory(
         schemaProvider,
         operationInvokers,
         vyneCacheConfiguration,
         vyneSpringProjectionConfiguration,
         formatSpecRegistry = formatSpecRegistry,
         operationCacheFactory = operationCacheFactory
      )
   }

   @Bean
   fun operationCacheFactory(providers: List<OperationCacheProviderBuilder>):OperationCacheFactory {
      log().info("Orbital has the following OperationCacheProviderBuilders registered: ${providers.joinToString { it::class.simpleName!! }}")
      return OperationCacheFactory(providers = providers)
   }

   @Bean
   fun localCacheProvider() = LocalCacheProviderBuilder()

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
