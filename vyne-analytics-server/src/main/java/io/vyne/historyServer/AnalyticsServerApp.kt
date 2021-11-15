package io.vyne.historyServer

import io.vyne.history.QueryAnalyticsConfig
import io.vyne.query.history.VyneHistoryRecord
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import io.vyne.spring.http.VyneQueryServiceExceptionProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Sinks

private val logger = KotlinLogging.logger {}

@SpringBootApplication
@EnableEurekaClient
@EnableConfigurationProperties(
   QueryAnalyticsConfig::class, VyneSpringHazelcastConfiguration::class
)
class AnalyticsServerApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(AnalyticsServerApp::class.java, *args)
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

      logger.info { "Analytics server version => $version" }
   }

   @Bean
   fun exceptionProvider() = VyneQueryServiceExceptionProvider()

   @Bean
   fun vyneHistoryRecordsSinks(): Sinks.Many<VyneHistoryRecord> = Sinks.many().multicast().directAllOrNothing()
}

@VyneSchemaPublisher
@VyneSchemaConsumer
@Configuration
class VyneConfig
