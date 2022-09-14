package io.vyne.schemaServer.core.openApi

import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration


// MP: 10-Aug-22: Have gutted this to a compilable minimum.  Will remove.
// Replace with Loaders and Transports.

@Configuration
class OpenApiConfiguration {
   private val logger = KotlinLogging.logger {}

   @Bean
   fun openApiVersionedSourceLoaders(
      config: OpenApiSchemaRepositoryConfig
   ): List<OpenApiVersionedSourceLoader> {

      if (config.services.isNotEmpty()) {
         logger.error { "Received legacy OpenApi config.  These configs are not loaded anymore.  Migrate to a supported transport (git / file / http) with an OpenApi adaptor" }

      }
      return emptyList()
//
//      return config.services.map {
//         OpenApiVersionedSourceLoader(
//            name = it.name,
//            url = URI(it.uri),
//            defaultNamespace = it.defaultNamespace,
//            connectTimeout = it.connectTimeout,
//            readTimeout = it.readTimeout,
//         )
//      }
   }
}

//
//
//
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
