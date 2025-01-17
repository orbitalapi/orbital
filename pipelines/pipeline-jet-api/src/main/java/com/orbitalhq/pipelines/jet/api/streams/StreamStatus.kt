package com.orbitalhq.pipelines.jet.api.streams

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.time.OffsetDateTime
import javax.print.attribute.standard.JobState

typealias StreamName = String
typealias JobId = String
@Entity(name = "stream_status")
data class StreamStatus(
   @Id
   val streamName: StreamName,
   /**
    * The stream state is the user-requested state, (eg, running or paused),
    * but is not neccessarily the same as the current job status - which reflects
    * how the stream engine has processed the stream over time.
    *
    * Note that this is not intended to display error statuses (which are states of the
    * underlying jobs), just the desired state. Interrogate the StreamJobStateEvent
    * for the stream to see the state of the jobs.
    */
   @Enumerated(EnumType.STRING)
   val state: State,
   val timestamp: OffsetDateTime = OffsetDateTime.now(),
   val username: String? = null
) {
   enum class State {
      RUNNING,
      PAUSED
   }
}

/**
 * A stream may have many jobs, and each of those
 * jobs goes through many states.
 */
data class StreamStateWithJobStates(
   val streamStatus: StreamStatus,
   // Each stream can have multiple jobs, as we cancel and restart
   // the stream.
   // Don't send this to the UI yet -- we can expose it if needed, but
   // could make payloads quite big
   private val jobStates: List<StreamJobState>
) {
   val jobState = jobStates.maxByOrNull { it.lastEvent.timestamp }
      ?.lastEvent
}

/**
 * Models events of the underlying Job(s) associated with a StreamStatus.
 * So, while a StreamStatus may have a desired state of "RUNNING",
 * it could go through several different states of JobStatus, and could also
 * have several different actual jobs over its lifecycle.
 */
data class StreamJobStateEvent(
   val jobId: JobId,
   val streamName: StreamName,
   val timestamp: OffsetDateTime = OffsetDateTime.now(),
   val status: JobStatus,
   val description: String? = null,

   ) {
   // These are a 1-to-1 mapping of Jet Job status.
   // Note that some of these we don't expect to occur
   enum class JobStatus {
      NOT_RUNNING,
      STARTING,
      RUNNING,
      SUSPENDED,
      SUSPENDED_EXPORTING_SNAPSHOT, // Shouldn't occur
      FAILED,
      COMPLETED // Shouldn't occur
   }
}

data class StreamJobState(
   val jobId: JobId,
   // Don't send this to the UI yet.
   // We can expose it if needed, but could make payloads quite big
   private val events: List<StreamJobStateEvent>
) {
   val lastEvent = events.maxByOrNull { it.timestamp }!!
}
