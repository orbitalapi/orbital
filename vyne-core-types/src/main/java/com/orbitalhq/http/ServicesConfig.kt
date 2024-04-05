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
      val METRICS_SERVER_NAME = "orbital-prometheus"

      val DEFAULT_QUERY_SERVER_RSOCKET_PORT = 7655
      val DEFAULT_STREAM_SERVER_RSOCKET_PORT = 7755

      val RSOCKET = "rsocket"
      val URL = "url"

      val DEFAULT = ServicesConfig(
         mapOf(
            ORBITAL_SERVER_NAME to mapOf(
               URL to "http://orbital:9022",
               RSOCKET to "tcp://orbital:$DEFAULT_QUERY_SERVER_RSOCKET_PORT"
            ),
            STREAM_SERVER_NAME to mapOf(
               URL to "http://orbital-stream-server",
               RSOCKET to "tcp://orbital-stream-server:$DEFAULT_STREAM_SERVER_RSOCKET_PORT"
            ),
            ANALYTICS_SERVER_NAME to mapOf(
               URL to "http://orbital-query-analytics",
               RSOCKET to "tcp://vyne-analytics-server:7654"
            ),
            METRICS_SERVER_NAME to mapOf(URL to "http://prometheus:9090")
         )
      )
   }
}
