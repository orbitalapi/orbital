package io.vyne.pipelines.orchestrator.runners

import io.vyne.pipelines.runner.PipelineInstanceReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PipelineRunnerApi(private val restTemplate: RestTemplate) {

   /**
    * This methods takes a pipelineDescription String and not a Pipeline as input. Explanation:
    * As soon as we cast the input string to anything more concrete, we lose the raw input which is what the downstream consumer requires.
    * If we use Pipeline, we might not send the full pipeline description to the runner.
    */
   fun submitPipeline(endpoint: String, pipelineDescription: String): PipelineInstanceReference {
      return restTemplate.postForObject(endpoint, pipelineDescription, PipelineInstanceReference::class.java)
   }
}
