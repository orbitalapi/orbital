package com.orbitalhq.history.rest

import com.orbitalhq.history.rest.export.QueryHistoryExporter
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@ConditionalOnExpression("\${vyne.db.enabled:true} == true and '\${vyne.analytics.mode:Inprocess}' == 'Inprocess'")
@Configuration
@ComponentScan(basePackageClasses=[QueryHistoryExporter::class, QueryHistoryService::class] )
class QueryHistoryRestConfig {
}
