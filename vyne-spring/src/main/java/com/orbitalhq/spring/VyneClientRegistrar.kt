package com.orbitalhq.spring

import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.VyneClient
import com.orbitalhq.embedded.EmbeddedVyneClient
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Schema
import com.orbitalhq.spring.config.VyneSpringProjectionConfiguration
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
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
      vyneSpringProjectionConfiguration: VyneSpringProjectionConfiguration,
      formatSpecRegistry: FormatSpecRegistry
   ): VyneClient {
      return EmbeddedVyneClient(
         VyneFactory(
            schemaProvider,
            operationInvokers,
            vyneCacheConfiguration,
            vyneSpringProjectionConfiguration,
            formatSpecRegistry = formatSpecRegistry
         )
      )
   }
}
