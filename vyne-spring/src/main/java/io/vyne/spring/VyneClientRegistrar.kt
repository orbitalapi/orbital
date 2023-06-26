package io.vyne.spring

import io.vyne.VyneCacheConfiguration
import io.vyne.VyneClient
import io.vyne.embedded.EmbeddedVyneClient
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(EnableEmbeddedVyneClientConfiguration::class)
annotation class EnableEmbeddedVyneClient

@Configuration
class EnableEmbeddedVyneClientConfiguration {
   @Bean
   fun vyneClient(
      schema: Schema,
      schemaProvider: SchemaProvider,
      operationInvokers: List<OperationInvoker>,
      vyneCacheConfiguration: VyneCacheConfiguration,
      vyneSpringProjectionConfiguration: VyneSpringProjectionConfiguration
   ): VyneClient {
      return EmbeddedVyneClient(
         VyneFactory(
            schemaProvider,
            operationInvokers,
            vyneCacheConfiguration,
            vyneSpringProjectionConfiguration
         )
      )
   }
}
