package io.vyne.queryService

import com.fasterxml.jackson.databind.MapperFeature
import com.netflix.discovery.EurekaClient
import io.vyne.VyneCacheConfiguration
import io.vyne.cask.api.CaskApi
import io.vyne.query.TaxiJacksonModule
import io.vyne.query.VyneJacksonModule
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.eureka.EurekaClientSchemaConsumer
import io.vyne.search.embedded.EnableVyneEmbeddedSearch
import io.vyne.spring.VYNE_SCHEMA_PUBLICATION_METHOD
import io.vyne.spring.VyneQueryServer
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.utils.log
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy
import org.apache.http.impl.client.HttpClients
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import reactivefeign.spring.config.EnableReactiveFeignClients
import javax.inject.Provider


@SpringBootApplication
@EnableConfigurationProperties(QueryServerConfig::class, VyneCacheConfiguration::class)
class QueryServiceApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(QueryServiceApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.run(*args)
      }
   }

   @Bean
   @ConditionalOnProperty(VYNE_SCHEMA_PUBLICATION_METHOD, havingValue = "EUREKA")
   fun eurekaClientConsumer(
      clientProvider: Provider<EurekaClient>,
      eventPublisher: ApplicationEventPublisher,
      @Value("\${vyne.taxi.rest.retry.count:3}") retryCount: Int): EurekaClientSchemaConsumer {
      val httpClient = HttpClients.custom()
         .setRetryHandler { _, executionCount, _ -> executionCount < retryCount }
         .setServiceUnavailableRetryStrategy(DefaultServiceUnavailableRetryStrategy(retryCount, 1000))
         .build()

      return EurekaClientSchemaConsumer(
         clientProvider,
         LocalValidatingSchemaStoreClient(),
         eventPublisher,
         RestTemplate(HttpComponentsClientHttpRequestFactory(httpClient)))
   }

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
      val version = if(!baseVersion.isNullOrEmpty() && buildNumber != "0" && buildInfo.version.contains("SNAPSHOT")) {
         "$baseVersion-BETA-$buildNumber"
      } else {
         buildInfo?.version ?: "Dev version"
      }

      log().info("Vyne query server $version")
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

      //      @Override
      //      public void addResourceHandlers(ResourceHandlerRegistry registry) {
      //         if (!registry.hasMappingForPattern("/**")) {
      //            registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
      //         }
      //      }


   }

   //@Configuration
   class FeignResponseDecoderConfig {

      //private val messageConverters =
      //   ObjectFactory { HttpMessageConverters() }

      /**
       * @return
       */
      //@Bean
      //fun feignEncoder(): Encoder? {
      //   return SpringEncoder(messageConverters)
      //}

      /**
       * @return
       */
      //@Bean
      //fun feignDecoder(): Decoder? {
      //   return SpringDecoder(messageConverters)
      //}

      //@Bean
      //fun feignDecoder(): Decoder {
      //   val messageConverters: ObjectFactory<HttpMessageConverters> =
      //      ObjectFactory<HttpMessageConverters> {
      //         val converters =
      //            HttpMessageConverters()
      //         converters
      //      }
       //  return SpringDecoder(messageConverters)
      //}
   }
}

@ConfigurationProperties(prefix = "vyne")
class QueryServerConfig {
   var newSchemaSubmissionEnabled: Boolean = false
}

@Configuration
@VyneSchemaPublisher
@VyneQueryServer
@EnableVyneEmbeddedSearch
class VyneConfig

@Configuration
@EnableReactiveFeignClients(clients = [CaskApi::class])
class FeignConfig
