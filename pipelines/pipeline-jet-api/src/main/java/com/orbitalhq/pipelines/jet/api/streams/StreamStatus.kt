package com.orbitalhq.pipelines.jet.api.streams

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.time.OffsetDateTime

@Entity(name = "stream_status")
data class StreamStatus(
   @Id
   val streamName: String,
   @Enumerated(EnumType.STRING)
   val state: State,
   val timestamp: OffsetDateTime = OffsetDateTime.now(),
   val username: String? = null
) {
   companion object {
      fun defaultPaused(name: String, username: String? = null): StreamStatus =
         StreamStatus(name, State.PAUSED, username = username)
   }

   enum class State {
      RUNNING,
      PAUSED
   }
}
