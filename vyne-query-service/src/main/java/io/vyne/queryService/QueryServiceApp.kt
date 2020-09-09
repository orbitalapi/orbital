package io.vyne.queryService

import com.netflix.discovery.EurekaClient
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
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.inject.Provider

@SpringBootApplication
@EnableConfigurationProperties(QueryServerConfig::class)
@EnableVyneEmbeddedSearch
@VyneSchemaPublisher
@EnableFeignClients(clients = [CaskApi::class])
@VyneQueryServer
@EnableAsync
class QueryServiceApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(QueryServiceApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.run(*args)
      }
   }

   @Bean("threadPoolTaskExecutor")
   fun getAsyncExecutor(): TaskExecutor? {
      val executor = ThreadPoolTaskExecutor()
      // TODO parameters should be in tune with the environment specs
      executor.corePoolSize = 20
      executor.maxPoolSize = 1000
      executor.threadNamePrefix = "Async-"
      executor.setWaitForTasksToCompleteOnShutdown(true)
      return executor
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
}

object AuthHeaders {
   val AUTH_HEADER_NAME = "Authorization"
}

@ConfigurationProperties(prefix = "vyne")
class QueryServerConfig {
   var newSchemaSubmissionEnabled: Boolean = false
}


