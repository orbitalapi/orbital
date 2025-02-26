package com.orbitalhq.pipelines.jet.streams

import com.orbitalhq.spring.config.RequiresOrbitalDbEnabled
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.TransactionManager

@RequiresOrbitalDbEnabled
@Configuration
class StreamStateManagerConfiguration {
    @Bean
    fun streamStatusMapStore(streamStatusRepository: StreamStatusRepository, unusedTransactionManager: TransactionManager) = StreamStatusMapStore(streamStatusRepository, unusedTransactionManager)
}