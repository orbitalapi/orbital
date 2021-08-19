package io.vyne.history.rest

import io.vyne.history.QueryHistoryExporter
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses=[QueryHistoryExporter::class, QueryHistoryService::class])
class QueryHistoryRestConfig {
}
