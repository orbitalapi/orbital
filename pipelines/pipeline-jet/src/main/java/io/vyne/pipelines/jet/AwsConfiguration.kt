package io.vyne.pipelines.jet

import io.micrometer.cloudwatch2.CloudWatchConfig
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import java.time.Duration

@Configuration
@Profile(value = ["aws"])
class AwsConfig {

   private val logger = KotlinLogging.logger {}

   @Bean
   fun currentAwsRegion(): Region {
      val defaultRegion = Region.US_EAST_1
      // Seems like the most reliable way of getting region is the AWS_REGION env var
      val envVarRegion = System.getenv("AWS_REGION")
      return if (envVarRegion.isNullOrEmpty()) {
         logger.warn { "AWS_REGION has not been set. Metrics will be associated with region $defaultRegion by default." }
         defaultRegion
      } else {
         try {
            logger.info { "Detected region $envVarRegion - using it for metrics." }
            Region.of(envVarRegion)
         } catch (e: Exception) {
            logger.warn(e) { "Failed to set region from environment variable of $envVarRegion. Default region $defaultRegion will be used." }
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
            "cloudwatch.namespace" to "pipelines",
            "cloudwatch.step" to Duration.ofMinutes(1).toString()
         )

         override fun get(key: String): String? {
            return configuration[key]
         }
      }
   }
}

data class MetricsTags(val tags: List<String>)
