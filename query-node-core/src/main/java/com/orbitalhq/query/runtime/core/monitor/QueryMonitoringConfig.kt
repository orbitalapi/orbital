package com.orbitalhq.query.runtime.core.monitor

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QueryMonitoringConfig {
   @Bean
   fun activeQueryMonitor() = ActiveQueryMonitor()
}
