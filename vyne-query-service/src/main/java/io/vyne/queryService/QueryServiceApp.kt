package io.vyne.queryService

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.orbital.station.OrbitalStationConfig
import io.vyne.cask.api.CaskApi
import io.vyne.cockpit.core.CockpitCoreConfig
import io.vyne.cockpit.core.FeatureTogglesConfig
import io.vyne.cockpit.core.WebUiUrlSupportFilter
import io.vyne.cockpit.core.lsp.LanguageServerConfig
import io.vyne.cockpit.core.pipelines.PipelineConfig
import io.vyne.cockpit.core.schemas.BuiltInTypesSubmitter
import io.vyne.cockpit.core.security.VyneUserConfig
import io.vyne.cockpit.core.telemetry.TelemetryConfig
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.history.db.InProcessHistoryConfiguration
import io.vyne.history.rest.QueryHistoryRestConfig
import io.vyne.licensing.LicenseConfig
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.format.ModelFormatSpec
import io.vyne.monitoring.EnableCloudMetrics
import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.transport.PipelineJacksonModule
import io.vyne.query.TaxiJacksonModule
import io.vyne.query.VyneJacksonModule
import io.vyne.query.chat.ChatQueryParser
import io.vyne.query.runtime.core.EnableVyneQueryNode
import io.vyne.schema.publisher.SchemaPublisherService
import io.vyne.schemaServer.changelog.ChangelogApi
import io.vyne.schemaServer.codegen.CodeGenApi
import io.vyne.schemaServer.editor.SchemaEditorApi
import io.vyne.schemaServer.packages.PackagesServiceApi
import io.vyne.schemaServer.repositories.RepositoryServiceApi
import io.vyne.search.embedded.EnableVyneEmbeddedSearch
import io.vyne.spring.EnableVyne
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.spring.config.ConditionallyLoadBalancedExchangeFilterFunction
import io.vyne.spring.config.DiscoveryClientConfig
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.auth.HttpAuthConfig
import io.vyne.spring.projection.ApplicationContextProvider
import io.vyne.spring.utils.versionOrDev
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
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
import org.springframework.web.reactive.function.client.WebClient
import reactivefeign.spring.config.EnableReactiveFeignClients
import java.util.*


@SpringBootApplication(scanBasePackageClasses = [QueryServiceApp::class, OrbitalStationConfig::class])
@EnableConfigurationProperties(
   VyneSpringCacheConfiguration::class,
   LanguageServerConfig::class,
   QueryAnalyticsConfig::class,
   PipelineConfig::class,
   VyneSpringProjectionConfiguration::class,
   VyneSpringHazelcastConfiguration::class,
   VyneUserConfig::class,
   FeatureTogglesConfig::class
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
      return ChatQueryParser(apiKey)
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

   //   @LoadBalanced
   @Bean
   fun webClientFactory(
      loadBalancingFilterFunction: ReactorLoadBalancerExchangeFilterFunction,
      metricsCustomizer: MetricsWebClientCustomizer,
      discoveryClient: DiscoveryClient
   ): WebClient.Builder {


      val builder = WebClient.builder()
         .filter(
            ConditionallyLoadBalancedExchangeFilterFunction.onlyKnownHosts(
               discoveryClient.services,
               loadBalancingFilterFunction
            )
         )
      metricsCustomizer.customize(builder)
      return builder
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
   QueryHistoryRestConfig::class,
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
      CaskApi::class,
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
