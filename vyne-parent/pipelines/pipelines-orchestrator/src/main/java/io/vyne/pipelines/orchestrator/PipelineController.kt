package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.runner.PipelineInstanceReference
import io.vyne.pipelines.runner.PipelineRunnerApi
import io.vyne.utils.log
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class PipelineController(val runner: PipelineRunnerApi) : PipelineRunnerApi {

   override fun submitPipeline(@RequestBody pipeline: Pipeline): PipelineInstanceReference {
      log().info("Received submitted pipeline: \n$pipeline")
      // TODO : Here, we'd want some way of storing which pipelines are running where.
      // However, ideally, we'd be using Eureka et al to track this in a distributed way, so that
      // instances are reporting which pipelines are running in a distributed manner,
      // which makes recovering from restarts a bit easier.
      return runner.submitPipeline(pipeline)
   }
}


