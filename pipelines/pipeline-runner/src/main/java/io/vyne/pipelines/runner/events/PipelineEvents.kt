package io.vyne.pipelines.runner.events

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.orchestrator.events.PipelineMessage
import io.vyne.pipelines.orchestrator.events.PipelineMessageEvent
import io.vyne.pipelines.orchestrator.events.PipelineStageEvent
import io.vyne.pipelines.orchestrator.events.PipelineStageStatus
import io.vyne.pipelines.orchestrator.jobs.PipelineJobId
import java.time.Instant


object PipelineJobIds {
   fun create(pipeline: Pipeline, message: PipelineInputMessage?): PipelineJobId {
      return if (message != null) {
         "${pipeline.id}:${message.id}"
      } else {
         pipeline.id
      }

   }
}

typealias PipelineStageObserverProvider = (stageName: String) -> PipelineStageObserver

class PipelineStageObserver private constructor(
   val stageName: String,
   val jobId: PipelineJobId,
   private val eventSink: PipelineEventSink
) : PipelineLogger {
   companion object {
      fun createStarted(stageName: String, jobId: PipelineJobId, eventSink: PipelineEventSink): PipelineStageObserver {
         return PipelineStageObserver(stageName, jobId, eventSink)
      }
   }

   fun <T> catchAndLog(message: String, method: () -> T): T {
      try {
         return method.invoke()
      } catch (exception: Exception) {
         this.error(exception) { message }
         throw(exception)
      }
   }

   override fun debug(message: () -> String) {
      sendEvent(PipelineMessage.Severity.DEBUG, message)
   }

   override fun info(message: () -> String) {
      sendEvent(PipelineMessage.Severity.INFO, message)
   }

   override fun warn(message: () -> String) {
      sendEvent(PipelineMessage.Severity.WARN, message)
   }

   override fun error(message: () -> String) {
      sendEvent(PipelineMessage.Severity.ERROR, message)
   }

   override fun error(exception: Throwable, message: () -> String) {
      sendEvent(PipelineMessage.Severity.ERROR, message, exception)
   }


   private fun sendEvent(severity: PipelineMessage.Severity, message: () -> String, exception: Throwable? = null) {
      eventSink.publish(
         PipelineMessageEvent(
            jobId,
            stageName,
            PipelineMessage.withThrowable(
               severity,
               Instant.now(),
               message(),
               exception
            )
         )
      )
   }

   fun completedSuccessfully() {
      finish(PipelineStageStatus.COMPLETED)
      info { "Completed" }
   }

   private fun finish(status: PipelineStageStatus) {
      eventSink.publish(PipelineStageEvent(
         jobId,
         stageName,
         Instant.now(),
         status
      ))
   }

   fun completedInError(exception: Throwable) {
      finish(PipelineStageStatus.FAILED)
      error(exception) { "Failed" }
   }
}
