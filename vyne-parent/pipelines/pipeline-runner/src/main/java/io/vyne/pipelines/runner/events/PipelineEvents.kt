package io.vyne.pipelines.runner.events

import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineLogger
import org.apache.commons.lang3.exception.ExceptionUtils
import org.reactivestreams.Publisher
import reactor.core.publisher.FluxSink
import java.time.Instant

typealias PipelineJobId = String

object PipelineJobIds {
   fun create(pipeline: Pipeline, message: PipelineInputMessage): PipelineJobId {
      return "${pipeline.id}:${message.id}"
   }
}

typealias PipelineStageObserverProvider = (stageName: String) -> PipelineStageObserver

class ObserverProvider(private val logger: PipelineLogger, private val eventSink: PipelineEventSink) {
   fun pipelineObserver(pipeline: Pipeline, message: PipelineInputMessage): PipelineStageObserverProvider {

      return { stageName ->
         pipelineStageObserver(PipelineJobIds.create(pipeline, message), stageName)
      }
   }

   private fun pipelineStageObserver(jobId: PipelineJobId, stageName: String): PipelineStageObserver {
      return PipelineStageObserver.createStarted(stageName, jobId, logger, eventSink)
   }
}

class PipelineStageObserver private constructor(
   val stageName: String,
   val jobId: PipelineJobId,
   private val logger: PipelineLogger,
   private val eventSink: PipelineEventSink
) : PipelineLogger by logger {
   companion object {
      fun createStarted(stageName: String, jobId: PipelineJobId, logger: PipelineLogger, eventSink: PipelineEventSink): PipelineStageObserver {
         return PipelineStageObserver(stageName, jobId, logger, eventSink)
      }
   }

   override fun debug(message: () -> String) {
      logger.debug(message)
      sendEvent(PipelineMessage.Severity.DEBUG, message)
   }

   override fun info(message: () -> String) {
      logger.info(message)
      sendEvent(PipelineMessage.Severity.INFO, message)
   }

   override fun warn(message: () -> String) {
      logger.warn(message)
      sendEvent(PipelineMessage.Severity.WARN, message)
   }

   override fun error(message: () -> String) {
      logger.error(message)
      sendEvent(PipelineMessage.Severity.ERROR, message)
   }

   override fun error(exception: Throwable, message: () -> String) {
      logger.error(exception, message)
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

interface PipelineEventSink {
   fun publish(event: PipelineEvent)
}

data class PipelineStageEvent(
   override val jobId: PipelineJobId,
   override val stageName: String,
   override val timestamp: Instant,
   val status: PipelineStageStatus
) : PipelineEvent {
   override val eventType: PipelineEvent.EventType = PipelineEvent.EventType.STAGE_EVENT
}

interface PipelineEvent {
   val eventType: EventType
   val jobId: PipelineJobId
   val timestamp: Instant
   val stageName: String

   enum class EventType {
      STAGE_EVENT,
      LOG_MESSAGE
   }
}

data class PipelineMessageEvent(
   override val jobId: PipelineJobId,
   override val stageName: String,
   val message: PipelineMessage
) : PipelineEvent {
   override val eventType: PipelineEvent.EventType = PipelineEvent.EventType.LOG_MESSAGE
   override val timestamp = message.timestamp
}

// even as I type this, it feels like a bad idea to duplicate
// the plethora of logging frameworks out there.
// £20 says this gets deleted.
data class PipelineMessage(
   val level: Severity,
   val timestamp: Instant,
   val message: String
) {
   companion object {
      fun withThrowable(
         level: Severity,
         timestamp: Instant,
         message: String,
         exception: Throwable? = null
      ): PipelineMessage {
         val logMessage = if (exception != null) {
            message + "\n" + ExceptionUtils.getMessage(exception) + "\n" + ExceptionUtils.getStackTrace(exception)
         } else {
            message
         }
         return PipelineMessage(level, timestamp, logMessage)
      }
   }

   enum class Severity {
      DEBUG,
      INFO,
      WARN,
      ERROR
   }
}

enum class PipelineStageStatus {
   NOT_STARTED,
   RUNNING,
   COMPLETED,
   FAILED
}
