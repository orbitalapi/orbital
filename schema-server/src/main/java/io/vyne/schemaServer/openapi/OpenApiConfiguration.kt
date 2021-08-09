package io.vyne.schemaServer.openapi

import io.vyne.schemaServer.CompileOnStartupListener
import io.vyne.schemaServer.CompilerService
import io.vyne.schemaServer.VersionedSourceLoader
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI
import java.time.Duration


@Configuration
class OpenApiConfiguration {

   @Bean
   fun openApiVersionedSourceLoaders(
      config: OpenApiServicesConfig
   ): List<OpenApiVersionedSourceLoader> =
      config.openApiServices.map {
         OpenApiVersionedSourceLoader(
            name = it.name,
            url = URI(it.uri),
            defaultNamespace = it.defaultNamespace,
            connectTimeout = it.connectTimeout,
            readTimeout = it.readTimeout,
         )
      }

   @Bean
   fun compileOnStartupListener(
      openApiVersionedSourceLoaders: List<OpenApiVersionedSourceLoader>,
      versionedSourceLoaders: List<VersionedSourceLoader>,
      compilerService: CompilerService,
   ): CompileOnStartupListener =
      CompileOnStartupListener(
         openApiVersionedSourceLoaders + versionedSourceLoaders,
         compilerService
      )
}

@ConstructorBinding
@ConfigurationProperties(prefix = "taxi")
data class OpenApiServicesConfig(
   val openApiServices: List<OpenApiServiceConfig> = emptyList()
) {
   @ConstructorBinding
   data class OpenApiServiceConfig(
      val name: String,
      val uri: String,
      val defaultNamespace: String,
      val connectTimeout: Duration = Duration.ofMillis(500),
      val readTimeout: Duration = Duration.ofSeconds(2),
   )
}
