package com.orbitalhq.cockpit.core.query

import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.query.connectors.OperationInvoker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ParserConfig {

   @Bean
   fun queryVisualizer(invokers: List<OperationInvoker>, stateStoreProvider: StateStoreProvider?): QueryVisualizer {
      return QueryVisualizer(invokers, stateStoreProvider)
   }

   @Bean
   fun queryInsightUtils(visualizerService: QueryVisualizer): QueryInsightUtils {
      return QueryInsightUtils(visualizerService)
   }
}
