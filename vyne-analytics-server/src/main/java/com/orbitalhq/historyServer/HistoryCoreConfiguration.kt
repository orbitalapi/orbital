package com.orbitalhq.historyServer

import com.orbitalhq.history.db.HistoryPersistenceJpaConfig
import com.orbitalhq.history.rest.QueryHistoryRestConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(*[HistoryPersistenceJpaConfig::class, QueryHistoryRestConfig::class])
@Configuration
class HistoryCoreConfiguration
