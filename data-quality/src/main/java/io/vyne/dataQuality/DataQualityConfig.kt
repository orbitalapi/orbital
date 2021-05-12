package io.vyne.dataQuality

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataQualityConfig {

   @Bean()
   fun ruleRegistry(): RuleRegistry {
      // Stubbed whilst spiking - this should be being loaded from the schema, or something.
      return RuleRegistry.default()
   }
}
