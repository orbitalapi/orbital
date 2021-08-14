package io.vyne.pipelines.runner

import io.vyne.pipelines.runner.transport.CompositeVariableSource
import io.vyne.pipelines.runner.transport.DefaultPipelineAwareVariableProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PipelineStateConfig {

   @Bean
   fun pipelineAwareVariableProvider() {
      DefaultPipelineAwareVariableProvider(
         pipelineState = mutableMapOf(), // TODO : This needs to become persistent somehow
         variableSource = CompositeVariableSource.withDefaults()
      )
   }
}
