package com.orbitalhq.queryService

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.orbital.station.OrbitalStationConfig
import com.orbitalhq.cockpit.core.CockpitCoreConfig
import com.orbitalhq.cockpit.core.FeatureTogglesConfig
import com.orbitalhq.cockpit.core.WebUiUrlSupportFilter
import com.orbitalhq.cockpit.core.lsp.LanguageServerConfig
import com.orbitalhq.cockpit.core.pipelines.PipelineConfig
import com.orbitalhq.cockpit.core.schemas.BuiltInTypesSubmitter
import com.orbitalhq.cockpit.core.security.VyneUserConfig
import com.orbitalhq.cockpit.core.telemetry.TelemetryConfig
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.history.db.InProcessHistoryConfiguration
import com.orbitalhq.history.noop.NoopQueryEventConsumerConfiguration
import com.orbitalhq.history.remote.RemoteHistoryConfig
import com.orbitalhq.history.rest.QueryHistoryRestConfig
import com.orbitalhq.licensing.LicenseConfig
import com.orbitalhq.models.csv.CsvFormatSpec
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.monitoring.EnableCloudMetrics
import com.orbitalhq.pipelines.jet.api.PipelineApi
import com.orbitalhq.pipelines.jet.api.transport.PipelineJacksonModule
import com.orbitalhq.query.TaxiJacksonModule
import com.orbitalhq.query.VyneJacksonModule
import com.orbitalhq.query.chat.ChatQueryParser
import com.orbitalhq.query.runtime.core.EnableVyneQueryNode
import com.orbitalhq.schema.publisher.SchemaPublisherService
import com.orbitalhq.schemaServer.changelog.ChangelogApi
import com.orbitalhq.schemaServer.codegen.CodeGenApi
import com.orbitalhq.schemaServer.editor.SchemaEditorApi
import com.orbitalhq.schemaServer.packages.PackagesServiceApi
import com.orbitalhq.schemaServer.repositories.RepositoryServiceApi
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.search.embedded.EnableVyneEmbeddedSearch
import com.orbitalhq.spring.EnableVyne
import com.orbitalhq.spring.VyneSchemaConsumer
import com.orbitalhq.spring.VyneSchemaPublisher
import com.orbitalhq.spring.config.*
import com.orbitalhq.spring.http.auth.HttpAuthConfig
import com.orbitalhq.spring.projection.ApplicationContextProvider
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import com.orbitalhq.spring.utils.versionOrDev
import com.orbitalhq.utils.log
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction
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
import org.springframework.web.reactive.config.WebFluxConfigurer
import reactivefeign.spring.config.EnableReactiveFeignClients
import java.util.*
import java.util.concurrent.TimeUnit


@SpringBootApplication(scanBasePackageClasses = [QueryServiceApp::class, OrbitalStationConfig::class])
@EnableConfigurationProperties(
   VyneSpringCacheConfiguration::class,
   LanguageServerConfig::class,
   QueryAnalyticsConfig::class,
   PipelineConfig::class,
   VyneSpringProjectionConfiguration::class,
   VyneSpringHazelcastConfiguration::class,
   VyneUserConfig::class,
   FeatureTogglesConfig::class,
   EnvVariablesConfig::class
)
@Import(
   HttpAuthConfig::class,
   ApplicationContextProvider::class,
   LicenseConfig::class,
   DiscoveryClientConfig::class,
   TelemetryConfig::class

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
   fun chatGptService(@Value("\${vyne.chat-gpt.api-key:''}") apiKey: String): ChatQueryParser {
      return ChatQueryParser(apiKey, OkHttpClient().newBuilder().readTimeout(30, TimeUnit.SECONDS).build())
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

   @Bean
   fun formatSpecRegistry(): FormatSpecRegistry = FormatSpecRegistry.default()

   @Bean
   fun webClientCustomizer(
      loadBalancingFilterFunction: ReactorLoadBalancerExchangeFilterFunction,
      discoveryClient: DiscoveryClient

   ): WebClientCustomizer {
      return WebClientCustomizer { webClientBuilder ->
         webClientBuilder.filter(
            ConditionallyLoadBalancedExchangeFilterFunction.onlyKnownHosts(
               discoveryClient.services,
               loadBalancingFilterFunction
            )
         )
      }
   }

   @Autowired
   fun logInfo(@Autowired(required = false) buildInfo: BuildProperties? = null) {
      log().info("Orbital Query Server v ${buildInfo.versionOrDev()}")
   }
}


@Configuration
@EnableVyne
@VyneSchemaConsumer
@VyneSchemaPublisher
@EnableVyneEmbeddedSearch
@EnableVyneQueryNode
@EnableCloudMetrics
@Import(
   InProcessHistoryConfiguration::class,
   NoopQueryEventConsumerConfiguration::class,
   QueryHistoryRestConfig::class,
   RemoteHistoryConfig::class,
   CockpitCoreConfig::class,
   WebUiUrlSupportFilter::class
)
class VyneConfig {
   @Bean
   fun builtInTypesSubmitter(publisher: SchemaPublisherService): BuiltInTypesSubmitter =
      BuiltInTypesSubmitter(publisher)
}

@Configuration
class PipelineConfig {
   @Bean
   fun pipelineModule(): PipelineJacksonModule = PipelineJacksonModule()
}

@Configuration
@EnableReactiveFeignClients(
   clients = [
      PipelineApi::class,
      SchemaEditorApi::class,
      PackagesServiceApi::class,
      RepositoryServiceApi::class,
      ChangelogApi::class,
      CodeGenApi::class
   ]
)
class FeignConfig

@Configuration
class WebFluxWebConfig(private val objectMapper: ObjectMapper) : WebFluxConfigurer {
   override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
      val defaults: ServerCodecConfigurer.ServerDefaultCodecs = configurer.defaultCodecs()
      defaults.jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
      // Spring Boot Admin 2.x
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

   override fun addCorsMappings(registry: org.springframework.web.reactive.config.CorsRegistry) {
      registry.addMapping("/**")
         .allowedOrigins("*")
         .allowedHeaders("*")
         .exposedHeaders("*")
         .allowedMethods("*")

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
class CustomWebFluxConfigSupport {

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
