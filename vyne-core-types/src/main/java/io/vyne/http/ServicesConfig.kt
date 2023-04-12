package io.vyne.http

import kotlinx.serialization.Serializable

@Serializable
data class ServicesConfig(
   val services: Map<String, Map<String, String>> = emptyMap()
) {
   companion object {
      val DEFAULT = ServicesConfig(
         mapOf(
            "schema-server" to mapOf("url" to "http://schema-server", "rsocket-port" to "7655"),
            "query-server" to mapOf("url" to "http://vyne"),
            "pipeline-runner" to mapOf("url" to "http://vyne-pipeline-runner"),
            "cask-server" to mapOf("url" to "http://cask"),
            "analytics-server" to mapOf("url" to "http://vyne-analytics-server")
         )
      )
   }
}
