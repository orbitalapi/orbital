package com.orbitalhq.query.runtime.core.monitor

import com.hazelcast.core.HazelcastInstance
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QueryMonitoringConfig {
   @Bean
   fun activeQueryMonitor(hazelcastInstance: HazelcastInstance) = ActiveQueryMonitor(hazelcastInstance)
}
