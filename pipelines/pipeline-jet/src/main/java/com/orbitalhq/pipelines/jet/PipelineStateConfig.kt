package com.orbitalhq.pipelines.jet

import com.orbitalhq.pipelines.jet.api.transport.CompositeVariableSource
import com.orbitalhq.pipelines.jet.api.transport.DefaultPipelineAwareVariableProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineAwareVariableProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PipelineStateConfig {

   @Bean
   fun pipelineAwareVariableProvider(): PipelineAwareVariableProvider {
      return DefaultPipelineAwareVariableProvider(
         pipelineState = mutableMapOf(), // TODO : This needs to become persistent somehow
         variableSource = CompositeVariableSource.withDefaults()
      )
   }
}
