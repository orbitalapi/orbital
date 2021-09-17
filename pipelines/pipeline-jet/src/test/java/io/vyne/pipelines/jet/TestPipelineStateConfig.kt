package io.vyne.pipelines.jet

import com.mercateo.test.clock.TestClock
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
   fun pipelineAwareVariableProvider(clock:Clock): PipelineAwareVariableProvider {
      return PipelineAwareVariableProvider.default(
         clock = clock
      )
   }
}
