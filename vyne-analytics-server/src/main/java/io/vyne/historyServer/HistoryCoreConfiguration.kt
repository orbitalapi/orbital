package io.vyne.historyServer

import io.vyne.history.db.HistoryPersistenceJpaConfig
import io.vyne.history.rest.QueryHistoryRestConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(*[HistoryPersistenceJpaConfig::class, QueryHistoryRestConfig::class])
@Configuration
class HistoryCoreConfiguration
