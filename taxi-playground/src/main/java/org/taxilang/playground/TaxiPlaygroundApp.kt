package org.taxilang.playground

import com.orbitalhq.playground.StubQueryService
import com.orbitalhq.serde.TaxiJacksonModule
import io.micrometer.cloudwatch2.CloudWatchConfig
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import java.time.Duration


@SpringBootApplication()
class TaxiPlaygroundApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(TaxiPlaygroundApp::class.java, *args)
      }
   }

   @Bean
   fun taxiJacksonModule() = TaxiJacksonModule

   @Bean
   fun restTemplate(): RestTemplate = RestTemplateBuilder().build()

   @Bean
   fun stubQueryService() = StubQueryService()
}

@Configuration
@Profile(value = ["aws"])
class AwsConfig {

   private val logger = KotlinLogging.logger {}

   @Bean
   fun currentAwsRegion(): Region {
      val defaultRegion = Region.US_EAST_1
      // Seems like the most reliable way of getting region
      // is the AWS_REGION env var.
      val envVarRegion = System.getenv("AWS_REGION")
      return if (envVarRegion.isNullOrEmpty()) {
         logger.warn { "Could not determine AWS_REGION env var.  Metrics will be associated with default of $defaultRegion" }
         defaultRegion
      } else {
         try {
            logger.info { "Detected region of $envVarRegion, will be used for metrics" }
            Region.of(envVarRegion)
         } catch (e: Exception) {
            logger.warn(e) { "Failed to set region from env var of $envVarRegion.  Will use default of $defaultRegion" }
            defaultRegion
         }
      }
   }

   @Bean
   fun cloudWatchAsyncClient(region: Region): CloudWatchAsyncClient {

      return CloudWatchAsyncClient
         .builder()
         .region(region)
         .credentialsProvider(
            ContainerCredentialsProvider.builder().build()
         )
         .build()
   }

   @Bean
   fun getMeterRegistry(client: CloudWatchAsyncClient): MeterRegistry {
      val cloudWatchConfig = setupCloudWatchConfig()
      return CloudWatchMeterRegistry(
         cloudWatchConfig,
         Clock.SYSTEM,
         client
      )
   }

   @Bean
   fun tags(region: Region) = MetricsTags(listOf("region", region.id()))


   private fun setupCloudWatchConfig(): CloudWatchConfig {
      return object : CloudWatchConfig {
         private val configuration = mapOf(
            "cloudwatch.namespace" to "voyagerApp",
            "cloudwatch.step" to Duration.ofMinutes(1).toString()
         )

         override fun get(key: String): String? {
            return configuration[key]
         }
      }
   }
}


@Configuration
class WebConfiguration : WebFluxConfigurer {

   override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
      configurer.customCodecs().register(CsvXmlMessageWriter())
   }
   override fun addCorsMappings(registry: CorsRegistry) {
      registry.addMapping("/**")
         .allowedMethods("*")
         .allowedHeaders("*")
         .allowedOrigins(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://localhost:3002",
            "http://localhost:3003",
            "https://playground.taxilang.org",
            "https://orbitalhq.com",
            "https://docs.orbitalhq.com",
            "https://taxilang.org",
            "https://docs.taxilang.org",
         )
         .exposedHeaders("x-query-id")
   }
}


data class MetricsTags(val tags: List<String>)

@Configuration
class UnsecureConfig {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   @Bean
   fun springWebFilterChainNoAuthentication(http: ServerHttpSecurity): SecurityWebFilterChain? {
      logger.warn { "Authentication is disabled" }
      return http
         .csrf().disable()
         .cors().disable()
         .headers().disable()
         .authorizeExchange()
         .anyExchange().permitAll()
         .and()
         .build()
   }
}
