package com.orbitalhq.http

import kotlinx.serialization.Serializable

@Serializable
data class ServicesConfig(
   val services: Map<String, Map<String, String>> = emptyMap()
) {
   companion object {
      val DEFAULT = ServicesConfig(
         mapOf(
            "schema-server" to mapOf("url" to "http://schema-server", "rsocket" to "tcp://schema-server:7655"),
            "query-server" to mapOf("url" to "http://orbital"),
            "pipeline-runner" to mapOf("url" to "http://orbital-pipeline-runner"),
            "cask-server" to mapOf("url" to "http://cask"),
            "analytics-server" to mapOf("url" to "http://vyne-analytics-server", "rsocket" to "tcp://vyne-analytics-server:7654")
         )
      )
   }
}
