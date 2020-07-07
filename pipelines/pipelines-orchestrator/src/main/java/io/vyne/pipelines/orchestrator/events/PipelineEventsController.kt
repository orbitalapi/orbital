package io.vyne.pipelines.orchestrator.events

import io.vyne.utils.log
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore

@RestController
@ApiIgnore
class PipelineEventsService : PipelineEventsApi {
   override fun publish(@RequestBody event: PipelineEvent) {
      log().info("$event")
   }

}
