package com.orbitalhq.connections

import java.time.Instant

data class ConnectionStatus(val status: Status, val message: String, val timestamp: Instant = Instant.now()) {
   companion object {
      fun unknown() = ConnectionStatus(Status.UNKNOWN, "Connection status unknown", Instant.now())
      fun healthy() = ConnectionStatus(Status.OK, "Healthy", Instant.now())
      fun error(message:String) = ConnectionStatus(Status.ERROR, message, Instant.now())
   }
   enum class Status {
      OK,
      ERROR,
      UNKNOWN,
      CONNECTING,
   }
}
