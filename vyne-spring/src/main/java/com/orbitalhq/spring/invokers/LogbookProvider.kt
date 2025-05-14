package com.orbitalhq.spring.invokers

import org.springframework.web.reactive.function.client.WebClient
import org.zalando.logbook.Correlation
import org.zalando.logbook.HttpHeaders
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.HttpResponse
import org.zalando.logbook.Logbook
import org.zalando.logbook.Precorrelation
import org.zalando.logbook.Sink
import org.zalando.logbook.spring.webflux.LogbookExchangeFilterFunction

/**
 * Modifies a Spring WebClient with Logbook, which provides a convenient
 * way to capture the actual request / response that was sent.
 *
 * Logbook also provides reasonable defaults for filtering sensitive headers
 * and tokens etc.
 *
 * In future, this should allow us to plug in custom filters for secure
 * concepts like headers, and control request/response header etc.
 */
class LogbookProvider {

   private fun newLogbook(): Pair<Logbook, CapturingSink> {
      val sink = CapturingSink()
      return Logbook.builder()
         .sink(sink)
         .build() to sink
   }

   fun modify(webClient: WebClient): Pair<WebClient, CapturingSink> {
      val (logbook, sink) = newLogbook()
      val updatedWebClient = webClient.mutate()
         .filter(LogbookExchangeFilterFunction(logbook))
         .build()
      return updatedWebClient to sink
   }
}

class CapturingSink : Sink {
   var request: HttpRequest? = null
      private set;
   var response: HttpResponse? = null
      private set;

   override fun write(precorrelation: Precorrelation, request: HttpRequest) {
   }

   fun requestHeaders(): Map<String, List<String>> {
      return if (request == null) {
         emptyMap()
      } else {
         this.request!!.headers.asVyneHeadersMap()
      }
   }
   fun responseHeaders(): Map<String, List<String>> {
      return if (response == null) {
         emptyMap()
      } else {
         this.response!!.headers.asVyneHeadersMap()
      }
   }

   override fun write(
      correlation: Correlation,
      request: HttpRequest,
      response: HttpResponse
   ) {
      this.request = request
      this.response = response
   }

}

fun HttpHeaders?.asVyneHeadersMap(): Map<String, List<String>> {
   return this?.toMutableMap()?.toMap() ?: emptyMap()
}
