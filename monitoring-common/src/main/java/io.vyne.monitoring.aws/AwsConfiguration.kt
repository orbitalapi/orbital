package com.orbitalhq.monitoring.aws

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

/**
 * In some build configs (where we're using native images),
 * we can't use Profiles.
 *
 *
 */
@Configuration
@Profile(value = ["aws"])
class AwsProfileConfig  : BaseAwsMetricsConfig(requireAwsRegionEnvVar = false)
open class BaseAwsMetricsConfig(
   /**
    * If set to true, only registers cloudwatch if the AWS_REGION env var
    * is explicitly set.
    *
    * Otherwise, we default to US_EAST_1 if we can't find one.
    *
    * This is used as a cheap way of detecting if we're really running in AWS.
    * For native images we can't use profiles, so we need a different way to avoid wiring AWS stuff
    * outside of AWS.
    */
   private val requireAwsRegionEnvVar:Boolean) {

   private val logger = KotlinLogging.logger {}

   @Bean
   open fun currentAwsRegion(): Region {
      val defaultRegion = Region.US_EAST_1
      return explicitAwsRegion ?: defaultRegion
   }

   private val explicitAwsRegion:Region?
      get() {
         // Seems like the most reliable way of getting region is the AWS_REGION env var
         val envVarRegion = System.getenv("AWS_REGION")
         return if (envVarRegion.isNullOrEmpty()) {
            logger.warn { "AWS_REGION has not been set." }
            null
         } else {
            try {
               logger.info { "Detected region $envVarRegion - using it for metrics." }
               Region.of(envVarRegion)
            } catch (e: Exception) {
               logger.warn(e) { "Failed to parse region from environment variable of $envVarRegion." }
               null
            }
         }
      }

   @Bean
   open fun cloudWatchAsyncClient(region: Region): CloudWatchAsyncClient? {
      if (explicitAwsRegion == null && requireAwsRegionEnvVar) {
         logger.warn { "No AWS Region detected - Cloudwatch metrics will be disabled" }
         return null
      } else {
         logger.info { "Configuring CloudWatch client for metrics in region $region" }
      }
      return CloudWatchAsyncClient
         .builder()
         .region(region)
         .credentialsProvider(
            ContainerCredentialsProvider.builder().build()
         )
         .build()
   }

   @Bean
   open fun getMeterRegistry(client: CloudWatchAsyncClient?): MeterRegistry? {
      if (client == null) {
         return null
      }
      logger.info { "CloudWatchMeterRegistry starting" }
      val cloudWatchConfig = setupCloudWatchConfig()
      return CloudWatchMeterRegistry(
         cloudWatchConfig,
         Clock.SYSTEM,
         client
      )
   }

   @Bean
   open fun tags(region: Region) = MetricsTags(listOf("region", region.id()))


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
