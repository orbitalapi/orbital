package io.vyne.pipelines.orchestrator.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.*
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.vyne.pipelines.orchestrator.jobs.PipelineJobId
import org.apache.commons.lang3.exception.ExceptionUtils
import java.time.Instant

data class PipelineStageEvent(
   override val jobId: PipelineJobId,
   override val stageName: String,
   override val timestamp: Instant,
   val status: PipelineStageStatus
) : PipelineEvent {
   override val eventType: PipelineEvent.EventType = PipelineEvent.EventType.STAGE_EVENT
}

@JsonTypeInfo(
   use = JsonTypeInfo.Id.NAME,
   include = JsonTypeInfo.As.EXISTING_PROPERTY,
   property = "eventType"
)
@JsonSubTypes(
   Type(PipelineMessageEvent::class, name = "LOG_MESSAGE"),
   Type(PipelineStageEvent::class, name = "STAGE_EVENT")
)
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
