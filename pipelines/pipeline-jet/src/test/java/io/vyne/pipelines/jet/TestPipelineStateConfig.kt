package io.vyne.pipelines.jet

import com.mercateo.test.clock.TestClock
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.vyne.connectors.jdbc.HikariJdbcConnectionFactory
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.pipelines.jet.api.transport.PipelineAwareVariableProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.OffsetDateTime

@Configuration
class TestPipelineStateConfig {

   @Bean
   fun clock(): Clock {
      return TestClock.fixed(OffsetDateTime.now())
   }

   @Bean
   fun pipelineAwareVariableProvider(clock: Clock): PipelineAwareVariableProvider {
      return PipelineAwareVariableProvider.default(
         clock = clock
      )
   }


   @Bean
   fun hikariConnectionFactory(
      connectionRegistry: InMemoryJdbcConnectionRegistry
   ): HikariJdbcConnectionFactory {
      return HikariJdbcConnectionFactory(
         connectionRegistry,
         HikariConfig(),
         MicrometerMetricsTrackerFactory(SimpleMeterRegistry())
      )
   }
}
