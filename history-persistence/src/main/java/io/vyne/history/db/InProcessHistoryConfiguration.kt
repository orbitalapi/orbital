package io.vyne.history.db

import io.micrometer.core.instrument.MeterRegistry
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.models.json.Jackson
import io.vyne.query.HistoryEventConsumerProvider
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Activates when Query history is handled in process.
 */
@ConditionalOnProperty(prefix = "vyne.analytics", name = ["mode"], havingValue = "Inprocess", matchIfMissing = true)
@Import(
   *[
      HistoryPersistenceJpaConfig::class,
// Used to be here.  Splitting out distinct responsibilities
// "thing that persists history"    and
// "thing that exposes a REST API to access history"
      // However, this might be the wrong call
//   QueryHistoryRestConfig::class
   ]
)
@Configuration
class InProcessHistoryConfiguration {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @Bean
   fun historyWriterProvider(
      queryHistoryRecordRepository: QueryHistoryRecordRepository,
      resultRowRepository: QueryResultRowRepository,
      lineageRecordRepository: LineageRecordRepository,
      remoteCallResponseRepository: RemoteCallResponseRepository,
      sankeyChartRowRepository: QuerySankeyChartRowRepository,
      config: QueryAnalyticsConfig = QueryAnalyticsConfig(),
      meterRegistry: MeterRegistry
   ): HistoryEventConsumerProvider {
      logger.info { "Analytics Data will be stored in a disk database." }
      return QueryHistoryDbWriter(
         queryHistoryRecordRepository,
         resultRowRepository,
         lineageRecordRepository,
         remoteCallResponseRepository,
         sankeyChartRowRepository,
         Jackson.defaultObjectMapper,
         config, meterRegistry
      )
   }
}
