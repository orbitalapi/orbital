package io.vyne.pipelines.runner.events

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineLogger
import java.time.Instant

typealias PipelineJobId = String

object PipelineJobIds {
   fun create(pipeline: Pipeline, message: PipelineInputMessage): PipelineJobId {
      return "${pipeline.id}:${message.id}"
   }
}

typealias PipelineStageObserverProvider = (stageName: String) -> PipelineStageObserver

class ObserverProvider(private val logger: PipelineLogger) {
   fun pipelineObserver(pipeline: Pipeline, message: PipelineInputMessage): PipelineStageObserverProvider {
      return { stageName ->
         pipelineStageObserver(PipelineJobIds.create(pipeline, message), stageName)
      }
   }

   private fun pipelineStageObserver(jobId: PipelineJobId, stageName: String): PipelineStageObserver {
      return PipelineStageObserver.createStarted(stageName, jobId, logger)
   }
}

class PipelineStageObserver private constructor(
   val stageName: String,
   val jobId: PipelineJobId,
   private val logger: PipelineLogger
) : PipelineLogger by logger {
   companion object {
      fun createStarted(stageName: String, jobId: PipelineJobId, logger: PipelineLogger): PipelineStageObserver {
         return PipelineStageObserver(stageName, jobId, logger)
      }
   }

   val startTime: Instant = Instant.now()
   var finishTime: Instant? = null

   fun completedSuccessfully() {
      finish()
      info { "Completed" }
   }

   private fun finish() {
      finishTime = Instant.now()
   }

   fun completedInError(exception: Throwable) {
      finish()
      error(exception) { "Failed" }
   }


}


// even as I type this, it feels like a bad idea to duplicate
// the plethora of logging frameworks out there.
// £20 says this gets deleted.
data class PipelineMessage(
   val level: Severity,
   val timestamp: Instant,
   val message: String,
   val exception: Exception? = null
) {
   enum class Severity {
      DEBUG,
      INFO,
      WARN,
      ERROR
   }

   override fun toString(): String {
      val logMessage = if (exception != null) {
         "$message \n ${exception.message} \n ${exception.stackTrace}"
      } else {
         message
      }
      return "$timestamp [$level] $logMessage"
   }
}

enum class PipelineStageStatus {
   NOT_STARTED,
   RUNNING,
   COMPLETED,
   FAILED
}
