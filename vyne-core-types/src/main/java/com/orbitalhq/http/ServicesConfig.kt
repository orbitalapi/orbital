package com.orbitalhq.http

import kotlinx.serialization.Serializable

@Serializable
data class ServicesConfig(
   val services: Map<String, Map<String, String>> = emptyMap()
) {
   companion object {
      @Deprecated("No external services need to contact Orbital")
      val ORBITAL_SERVER_NAME = "orbital-server"
      @Deprecated("Stream server is no longer an external service")
      val STREAM_SERVER_NAME = "stream-server"
      @Deprecated("Analytics server is no longer an external service")
      val ANALYTICS_SERVER_NAME = "analytics-server"
      val METRICS_SERVER_NAME = "orbital-prometheus"

      val NEBULA_SERVER_NAME = "nebula"

      val DEFAULT_QUERY_SERVER_RSOCKET_PORT = 7655
      val DEFAULT_STREAM_SERVER_RSOCKET_PORT = 7755

      val RSOCKET = "rsocket"
      val URL = "url"

      val DEFAULT = ServicesConfig(
         mapOf(
            METRICS_SERVER_NAME to mapOf(URL to "http://prometheus:9090"),
            NEBULA_SERVER_NAME to mapOf(URL to "http://nebula:8099")
         )
      )
   }
}
