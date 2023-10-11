package com.orbitalhq.history.rest

import com.orbitalhq.history.rest.export.QueryHistoryExporter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@ConditionalOnProperty(prefix = "vyne.analytics", name = ["mode"], havingValue = "Inprocess", matchIfMissing = true)
@Configuration
@ComponentScan(basePackageClasses=[QueryHistoryExporter::class, QueryHistoryService::class] )
class QueryHistoryRestConfig {
}
