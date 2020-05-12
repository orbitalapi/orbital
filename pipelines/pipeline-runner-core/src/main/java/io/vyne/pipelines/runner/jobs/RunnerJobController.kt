package io.vyne.pipelines.runner.jobs

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.runner.PipelineBuilder
import io.vyne.pipelines.runner.PipelineInstanceReference
import io.vyne.pipelines.runner.PipelineRunnerApi
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class RunnerJobController(private val pipelineBuilder: PipelineBuilder) : PipelineRunnerApi {
   override fun submitPipeline(@RequestBody pipeline: Pipeline): PipelineInstanceReference {
      val instance = pipelineBuilder.build(pipeline)
      return instance
   }

}
