package io.vyne.pipelines.runner.jobs

import com.netflix.appinfo.ApplicationInfoManager
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.runner.PipelineBuilder
import io.vyne.pipelines.runner.PipelineInstanceReference
import io.vyne.pipelines.runner.PipelineRunnerApi
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class RunnerJobController(private val pipelineBuilder: PipelineBuilder, val appInfoManager: ApplicationInfoManager) : PipelineRunnerApi {



   override fun submitPipeline(@RequestBody pipeline: Pipeline): PipelineInstanceReference {
      val instance = pipelineBuilder.build(pipeline)

      appInfoManager.registerAppMetadata(mapOf(pipeline.input.transport.type to pipeline.output.transport.type))
      return instance
   }

}
