package io.vyne.queryService

import io.micrometer.core.instrument.MeterRegistry
import io.vyne.history.QueryHistoryConfig
import io.vyne.history.db.HistoryPersistenceJpaConfig
import io.vyne.history.db.LineageRecordRepository
import io.vyne.history.db.QueryHistoryDbWriter
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.history.db.RemoteCallResponseRepository
import io.vyne.history.rest.QueryHistoryRestConfig
import io.vyne.models.json.Jackson
import io.vyne.query.HistoryEventConsumerProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Activates when Query history is handled in process.
 */
@ConditionalOnProperty(prefix = "vyne.history", name = ["mode"], havingValue = "inprocess", matchIfMissing = true)
@Import(*[HistoryPersistenceJpaConfig::class, QueryHistoryRestConfig::class])
@Configuration
class InProcessHistoryConfiguration {
   @Bean
   fun historyWriterProvider(
      queryHistoryRecordRepository: QueryHistoryRecordRepository,
      resultRowRepository: QueryResultRowRepository,
      lineageRecordRepository: LineageRecordRepository,
      remoteCallResponseRepository: RemoteCallResponseRepository,
      config: QueryHistoryConfig = QueryHistoryConfig(),
      meterRegistry: MeterRegistry
   ): HistoryEventConsumerProvider =
      QueryHistoryDbWriter(
         queryHistoryRecordRepository,
         resultRowRepository,
         lineageRecordRepository,
         remoteCallResponseRepository,
         Jackson.defaultObjectMapper,
         config, meterRegistry)
}
