package com.orbitalhq.telemetry

import com.posthog.java.PostHog
import org.springframework.beans.factory.DisposableBean


// Groups isn't officially supported by the PostHog Java SDK
// However, see here for how to send it anyway:
// https://github.com/PostHog/posthog-java/issues/26
class PosthogTelemetryService(
   private val organisationName: String,
   private val meta: AnalyticsMeta,
   private val apiKey: String = POSTHOG_API_ID,
   private val host: String = POSTHOG_ENDPOINT,

   ) : DisposableBean, TelemetryService {


   override val defaultSessionId: String = organisationName.replace(" ", "-").lowercase()
   private val eventMeta = mapOf(
      "\$groups" to mapOf(
         "groupId" to defaultSessionId,
      ),
      "meta" to mapOf(
         "version" to meta.version,
         "product" to meta.productName,
         "hasLicense" to meta.hasLicense
      )
   )

   private val posthog = PostHog.Builder(apiKey)
      .host(host)
      .build()

   companion object {
      private const val POSTHOG_API_ID = "phc_pHqru8O89aZ9yu2fVczPh20GiFcuTXgFDIpkbTcS5vX"
      private const val POSTHOG_ENDPOINT = "https://app.posthog.com"
   }

   override fun record(eventId: String, data: Map<String, Any>, sessionId: String) {
      val eventData = buildEventData(data)
      posthog.capture(sessionId, eventId, eventData)
   }

   private fun buildEventData(data: Map<String, Any>): Map<String, Any> {
      return data + eventMeta
   }

   override fun destroy() {
      posthog.shutdown()
   }
}

interface TelemetryService {

   val defaultSessionId: String
   fun record(eventId: String, data: Map<String, Any> = emptyMap(), sessionId: String = defaultSessionId)
}

object NoopTelemetryService : TelemetryService {
   override val defaultSessionId: String = ""

   override fun record(eventId: String, data: Map<String, Any>, sessionId: String) {
   }

}

data class AnalyticsMeta(
   val productName: String,
   val version: String,
   val hasLicense: Boolean
)
