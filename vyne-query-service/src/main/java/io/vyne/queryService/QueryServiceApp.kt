package io.vyne.queryService

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.cask.api.CaskApi
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.licensing.LicenseConfig
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.format.ModelFormatSpec
import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.transport.PipelineJacksonModule
import io.vyne.query.TaxiJacksonModule
import io.vyne.query.VyneJacksonModule
import io.vyne.queryService.lsp.LanguageServerConfig
import io.vyne.queryService.pipelines.PipelineConfig
import io.vyne.queryService.security.VyneUserConfig
import io.vyne.schemaServer.editor.SchemaEditorApi
import io.vyne.search.embedded.EnableVyneEmbeddedSearch
import io.vyne.spring.EnableVyne
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.spring.config.DiscoveryClientConfig
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.auth.HttpAuthConfig
import io.vyne.spring.projection.ApplicationContextProvider
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.http.codec.EncoderHttpMessageWriter
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import org.springframework.web.reactive.config.WebFluxConfigurationSupport
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import reactivefeign.spring.config.EnableReactiveFeignClients
import reactor.core.publisher.Mono
import java.util.Collections


@SpringBootApplication
@EnableConfigurationProperties(
   QueryServerConfig::class,
   VyneSpringCacheConfiguration::class,
   LanguageServerConfig::class,
   QueryAnalyticsConfig::class,
   PipelineConfig::class,
   VyneSpringProjectionConfiguration::class,
   VyneSpringHazelcastConfiguration::class,
   VyneUserConfig::class,
)
@Import(
   HttpAuthConfig::class,
   ApplicationContextProvider::class,
   LicenseConfig::class,
   DiscoveryClientConfig::class
)
class QueryServiceApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
//         DebugProbes.install()
//         DebugProbes.enableCreationStackTraces = false
         val app = SpringApplication(QueryServiceApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.run(*args)
      }
   }


   @Bean
   fun csvFormatSpec(): ModelFormatSpec = CsvFormatSpec

   @Bean
   fun vyneJacksonModule() = VyneJacksonModule()

   @Bean
   fun taxiJacksonModule() = TaxiJacksonModule()

   @Bean
   fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
      return Jackson2ObjectMapperBuilderCustomizer { builder ->
         builder.featuresToEnable(
            MapperFeature.DEFAULT_VIEW_INCLUSION
         )
      }
   }

   @Autowired
   fun logInfo(@Autowired(required = false) buildInfo: BuildProperties? = null) {
      val baseVersion = buildInfo?.get("baseVersion")
      val buildNumber = buildInfo?.get("buildNumber")
      val version = if (!baseVersion.isNullOrEmpty() && buildNumber != "0" && buildInfo.version.contains("SNAPSHOT")) {
         "$baseVersion-BETA-$buildNumber"
      } else {
         buildInfo?.version ?: "Dev version"
      }

      log().info("Vyne query server version => $version")
   }

   @Configuration
   //   @EnableWebMvc
   class WebConfig : WebMvcConfigurer {

      @Value("\${cors.host:localhost}")
      lateinit var allowedHost: String

      @Value("\${cors.enabled:false}")
      var corsEnabled: Boolean = false

      @Value("\${vyne.mvc.executor.corePoolSize:5}")
      var corePoolSize: Int = 5

      @Value("\${vyne.mvc.executor.maxPoolSize:5}")
      var maxPoolSize: Int = 15

      @Value("\${vyne.mvc.executor.queueCapacity:50}")
      var queueCapacity: Int = 50

      override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
         converters.sortBy { converter -> if (converter is KotlinSerializationJsonHttpMessageConverter) 1000 else 0 }
      }

      override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
         val executor = ThreadPoolTaskExecutor()
         executor.corePoolSize = corePoolSize
         executor.maxPoolSize = maxPoolSize // maximum number of concurrent running threads when queue size is full
         executor.setQueueCapacity(queueCapacity)
         executor.threadNamePrefix = "vyne-query-executor"
         executor.initialize()
         configurer.setTaskExecutor(executor)
      }

      override fun addCorsMappings(registry: CorsRegistry) {
         if (corsEnabled) {
            log().info("Registering Cors host at $allowedHost")
            registry.addMapping("/**")
               .allowedOrigins(allowedHost)
         }
      }
   }

}


/**
 * Handles requests intended for our web app (ie., everything not at /api)
 * and forwards them down to index.html, to allow angular to handle the
 * routing
 */
@Component
class Html5UrlSupportFilter(
   @Value("\${management.endpoints.web.base-path:/actuator}") private val actuatorPath: String
) : WebFilter {
   companion object {
      val ASSET_EXTENSIONS =
         listOf(".css", ".js", ".js?", ".js.map", ".html", ".scss", ".ts", ".ttf", ".wott", ".svg", ".gif", ".png")
   }

   override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
      val path = exchange.request.uri.path
      // If the request is not for the /api, and does not contain a . (eg., main.js), then
      // redirect to index.  This means requrests to things like /query-wizard are rendereed by our Angular app
      return when {
         path.startsWith("/api") -> {
            chain.filter(exchange)
         }
         path.startsWith(actuatorPath) -> {
            chain.filter(exchange)
         }
         ASSET_EXTENSIONS.any { path.endsWith(it) } -> chain.filter(exchange)
         else -> {
            // These are requests that aren't /api, and don't have an asset extension (like .js), so route it to the
            // angular app
            chain.filter(
               exchange
                  .mutate().request(
                     exchange.request.mutate().path("/index.html").build()
                  )
                  .build()
            )
         }
      }
   }
}

@ConfigurationProperties(prefix = "vyne")
class QueryServerConfig {
   var newSchemaSubmissionEnabled: Boolean = false
}

@Configuration
@EnableVyne
@VyneSchemaConsumer
@VyneSchemaPublisher
@EnableVyneEmbeddedSearch
class VyneConfig

@Configuration
class PipelineConfig {
   @Bean
   fun pipelineModule(): PipelineJacksonModule = PipelineJacksonModule()
}

@Configuration
@EnableReactiveFeignClients(clients = [CaskApi::class, PipelineApi::class, SchemaEditorApi::class])
class FeignConfig

@Configuration
class WebFluxWebConfig(private val objectMapper: ObjectMapper) : WebFluxConfigurer {
   override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
      val defaults: ServerCodecConfigurer.ServerDefaultCodecs = configurer.defaultCodecs()
      defaults.jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
      // SPring Boot Admin 2.x
      // checks for the content-type application/vnd.spring-boot.actuator.v2.
      // If this content-type is absent, the application is considered to be a Spring Boot 1 application.
      // Spring Boot Admin can't display the metrics with Metrics are not supported for Spring Boot 1.x applications.
      defaults.jackson2JsonEncoder(
         Jackson2JsonEncoder(
            objectMapper,
            MediaType.APPLICATION_JSON,
            ActuatorV2MediaType,
            ActuatorV3MediaType
         )
      )

   }


   companion object {
      private val ActuatorV2MediaType = MediaType("application", "vnd.spring-boot.actuator.v2+json")
      private val ActuatorV3MediaType = MediaType("application", "vnd.spring-boot.actuator.v3+json")
   }
}


/**
 * Workaround to Spring 5.3 ordering of codecs, to favour Jacckson over Kotlin
 *
 * In Spring 5.3 it appears the KotlinSerializationJsonEncoder is weighted higher
 * than Jackson2JsonEncoder.
 *
 * This means if we try to return a class that is tagged with Kotlin's @Serializable annotation,
 * Spring will use Kotlin, rather than Jackson.
 *
 * This is undesirable, and causes serialization issues.  We also have a number of custom
 * Jackon serializers written, which we want to use.
 *
 * In Spring Reactive, there's no easy way to modify ordering of Codecs.
 * So, we use this adapter to swap out the order of the codecs, pushing Jackson to the front.
 *
 * https://github.com/spring-projects/spring-framework/issues/28856
 */
@Configuration
class CustomerWebFluxConfigSupport {

   @Bean
   @Primary
   fun serverCodecConfigurerAdapter(other: ServerCodecConfigurer): ServerCodecConfigurer {
      return ReOrderingServerCodecConfigurer(other)
   }

   class ReOrderingServerCodecConfigurer(private val configurer: ServerCodecConfigurer) :
      ServerCodecConfigurer by configurer {

      override fun getWriters(): MutableList<HttpMessageWriter<*>> {
         val writers = configurer.writers
         val jacksonWriterIndex =
            configurer.writers.indexOfFirst { it is EncoderHttpMessageWriter && it.encoder is Jackson2JsonEncoder }
         val kotlinSerializationWriterIndex =
            configurer.writers.indexOfFirst { it is EncoderHttpMessageWriter && it.encoder is KotlinSerializationJsonEncoder }

         if (kotlinSerializationWriterIndex == -1 || jacksonWriterIndex == -1) {
            return writers
         }

         if (kotlinSerializationWriterIndex < jacksonWriterIndex) {
            Collections.swap(writers, jacksonWriterIndex, kotlinSerializationWriterIndex)
         }
         return writers
      }
   }
}
