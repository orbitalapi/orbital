package com.orbitalhq.historyServer

import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.models.csv.CsvFormatSpec
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.query.history.VyneHistoryRecord
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import com.orbitalhq.spring.VyneSchemaConsumer
import com.orbitalhq.spring.VyneSchemaPublisher
import com.orbitalhq.spring.config.DiscoveryClientConfig
import com.orbitalhq.spring.config.VyneSpringHazelcastConfiguration
import com.orbitalhq.spring.http.VyneQueryServiceExceptionProvider
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import com.orbitalhq.utils.log
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import reactor.core.publisher.Sinks
import java.util.*

private val logger = KotlinLogging.logger {}

@SpringBootApplication
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

}

@VyneSchemaPublisher
@VyneSchemaConsumer
@Configuration
class VyneConfig


@Configuration
@Import(DiscoveryClientConfig::class)
class DiscoveryConfig


@Configuration
class AnalyticsServiceConfig {

   @Bean
   fun exceptionProvider() = VyneQueryServiceExceptionProvider()

   @Bean
   fun vyneHistoryRecordsSinks(): Sinks.Many<VyneHistoryRecord> = Sinks.many().multicast().directAllOrNothing()

   @Bean
   fun csvFormatSpec(): ModelFormatSpec = CsvFormatSpec

   @Bean
   fun formatSpecRegistry(): FormatSpecRegistry = FormatSpecRegistry.default()

   @Bean
   fun sourceConverterRegistry(): SourceConverterRegistry = SourceConverterRegistry(
      setOf(
         TaxiSourceConverter,
//         SoapWsdlSourceConverter
      ),
      registerWithStaticRegistry = true
   )

}
