package io.vyne.spring

import io.vyne.VyneCacheConfiguration
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemaStore.SchemaSourceProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

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
}
