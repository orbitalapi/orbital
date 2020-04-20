package io.vyne.pipelines.runner.events

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.orchestrator.jobs.PipelineJobId
import org.springframework.stereotype.Component

@Component
class ObserverProvider(private val eventSink: PipelineEventSink) {
   companion object {
      fun local():ObserverProvider {
         return ObserverProvider(CollectingEventSink())
      }
   }
   fun pipelineObserver(pipeline: Pipeline, message: PipelineInputMessage?): PipelineStageObserverProvider {

      return { stageName ->
         pipelineStageObserver(PipelineJobIds.create(pipeline, message), stageName)
      }
   }

   private fun pipelineStageObserver(jobId: PipelineJobId, stageName: String): PipelineStageObserver {
      return PipelineStageObserver.createStarted(stageName, jobId, eventSink)
   }
}
