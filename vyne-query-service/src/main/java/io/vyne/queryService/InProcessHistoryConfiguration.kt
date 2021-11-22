package io.vyne.queryService

import io.micrometer.core.instrument.MeterRegistry
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.history.db.HistoryPersistenceJpaConfig
import io.vyne.history.db.LineageRecordRepository
import io.vyne.history.db.QueryHistoryDbWriter
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.history.db.QuerySankeyChartRowRepository
import io.vyne.history.db.RemoteCallResponseRepository
import io.vyne.history.rest.QueryHistoryRestConfig
import io.vyne.models.json.Jackson
import io.vyne.query.HistoryEventConsumerProvider
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
private val logger = KotlinLogging.logger {}
/**
 * Activates when Query history is handled in process.
 */
@ConditionalOnProperty(prefix = "vyne.analytics", name = ["mode"], havingValue = "Inprocess", matchIfMissing = true)
@Import(*[HistoryPersistenceJpaConfig::class, QueryHistoryRestConfig::class])
@Configuration
class InProcessHistoryConfiguration {
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
      logger.info { "Analytics Data will be stored by Vyne Query Server." }
      return QueryHistoryDbWriter(
         queryHistoryRecordRepository,
         resultRowRepository,
         lineageRecordRepository,
         remoteCallResponseRepository,
         sankeyChartRowRepository,
         Jackson.defaultObjectMapper,
         config, meterRegistry)
   }
}
