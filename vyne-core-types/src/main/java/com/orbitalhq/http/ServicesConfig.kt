package com.orbitalhq.http

import kotlinx.serialization.Serializable

@Serializable
data class ServicesConfig(
   val services: Map<String, Map<String, String>> = emptyMap()
) {
   companion object {
      val ORBITAL_SERVER_NAME = "orbital-server"
      val STREAM_SERVER_NAME = "stream-server"
      val ANALYTICS_SERVER_NAME = "analytics-server"
      val METRICS_SERVER_NAME = "orbital-metrics-server"

      val DEFAULT_QUERY_SERVER_RSOCKET_PORT = 7655

      val DEFAULT = ServicesConfig(
         mapOf(
            ORBITAL_SERVER_NAME to mapOf(
               "url" to "http://orbital",
               "rsocket" to "tcp://orbital:$DEFAULT_QUERY_SERVER_RSOCKET_PORT"
            ),
            STREAM_SERVER_NAME to mapOf("url" to "http://orbital-stream-server"),
            ANALYTICS_SERVER_NAME to mapOf(
               "url" to "http://orbital-query-analytics",
               "rsocket" to "tcp://vyne-analytics-server:7654"
            ),
            "orbital-metrics-server" to mapOf("url" to "http://orbital-metrics")
         )
      )
   }
}
