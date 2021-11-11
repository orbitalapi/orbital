package io.vyne.schemaServer.openapi

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI
import java.time.Duration


@Configuration
class OpenApiConfiguration {

   @Bean
   fun openApiVersionedSourceLoaders(
      config: OpenApiSchemaRepositoryConfig
   ): List<OpenApiVersionedSourceLoader> {
      return config.services?.map {
         OpenApiVersionedSourceLoader(
            name = it.name,
            url = URI(it.uri),
            defaultNamespace = it.defaultNamespace,
            connectTimeout = it.connectTimeout,
            readTimeout = it.readTimeout,
         )
      }
   }


}

data class OpenApiSchemaRepositoryConfig(
   val pollFrequency: Duration = Duration.ofSeconds(20),
   val services: List<OpenApiServiceConfig> = emptyList()
) {
   data class OpenApiServiceConfig(
      val name: String,
      val uri: String,
      val defaultNamespace: String,
      val connectTimeout: Duration = Duration.ofMillis(500),
      val readTimeout: Duration = Duration.ofSeconds(2),
   )
}