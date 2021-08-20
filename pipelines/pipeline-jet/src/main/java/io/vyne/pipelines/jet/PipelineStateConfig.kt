package io.vyne.pipelines.jet

import io.vyne.pipelines.runner.transport.CompositeVariableSource
import io.vyne.pipelines.runner.transport.DefaultPipelineAwareVariableProvider
import io.vyne.pipelines.runner.transport.PipelineAwareVariableProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PipelineStateConfig {

   @Bean
   fun pipelineAwareVariableProvider():PipelineAwareVariableProvider {
      return DefaultPipelineAwareVariableProvider(
         pipelineState = mutableMapOf(), // TODO : This needs to become persistent somehow
         variableSource = CompositeVariableSource.withDefaults()
      )
   }
}
