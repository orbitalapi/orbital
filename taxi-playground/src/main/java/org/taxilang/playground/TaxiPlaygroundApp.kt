package org.taxilang.playground

import io.micrometer.cloudwatch2.CloudWatchConfig
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.query.TaxiJacksonModule
import mu.KotlinLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import java.time.Duration


@SpringBootApplication
class TaxiPlaygroundApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(TaxiPlaygroundApp::class.java, *args)
      }
   }

   @Bean
   fun taxiJacksonModule() = TaxiJacksonModule()

   @Bean
   fun restTemplate(): RestTemplate = RestTemplateBuilder().build()
}

@Configuration
@Profile("!dev")
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

         override fun get(key: String): String {
            return configuration[key]!!
         }
      }
   }
}


@Configuration
@Profile("dev")
class WebConfiguration : WebFluxConfigurer {
   override fun addCorsMappings(registry: CorsRegistry) {
      registry.addMapping("/**").allowedMethods("*")
   }

   @Bean
   fun tags() = MetricsTags(listOf("region", "dev"))
}


data class MetricsTags(val tags: List<String>)
