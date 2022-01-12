package io.vyne.historyServer

import io.vyne.history.QueryAnalyticsConfig
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.format.ModelFormatSpec
import io.vyne.query.history.VyneHistoryRecord
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import io.vyne.spring.http.VyneQueryServiceExceptionProvider
import io.vyne.utils.log
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
import java.util.TimeZone
import javax.annotation.PostConstruct

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

   @PostConstruct
   fun setUtcTimezone() {
      log().info("Setting default TimeZone to UTC")
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
      // Date values are stored in Postgresql in UTC.
      // However before sending dates to user a conversion happens (PgResultSet.getDate..) that includes default timezone
      // E.g. date 2020.03.29:00:00:00 in DB can converted and returned to user as 2020.03.28:23:00:00
      // This fix forces default timezone to be UTC
      // Alternatively we could provide -Duser.timezone=UTC at startup
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

   @Bean
   fun csvFormatSpec(): ModelFormatSpec = CsvFormatSpec
}

@VyneSchemaPublisher
@VyneSchemaConsumer
@Configuration
class VyneConfig
