package com.orbitalhq.spring.invokers

import com.google.common.base.Throwables
import com.orbitalhq.query.tracing.ConnectionError
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEvent
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schemas.Type
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.zalando.logbook.Correlation
import org.zalando.logbook.HttpHeaders
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.HttpResponse
import org.zalando.logbook.Logbook
import org.zalando.logbook.Precorrelation
import org.zalando.logbook.Sink
import org.zalando.logbook.spring.webflux.LogbookExchangeFilterFunction
import reactor.core.publisher.Mono

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

   private fun newLogbook(
      span: OperationTraceSpan,
      requestBodyType: Type?,
   ): Pair<Logbook, CapturingSink> {
      val sink = CapturingSink(span, requestBodyType)
      return Logbook.builder()
         .sink(sink)
         .build() to sink
   }

   fun modify(
      webClient: WebClient,
      span: OperationTraceSpan,
      requestBodyType: Type?,
   ): Pair<WebClient, CapturingSink> {
      val (logbook, sink) = newLogbook(span, requestBodyType)
      val updatedWebClient = webClient.mutate()
         // Order matters here, make sure ConnectionErrorTracingFilterFunction is added first, as
         // filters are applied bottom-up, and we want to make sure the request event is emitted
         // before the error event
         .filter(ConnectionErrorTracingFilterFunction(span))
         .filter(LogbookExchangeFilterFunction(logbook))
         .build()
      return updatedWebClient to sink
   }
}

/**
 * Captures connection errors only, and emits trace events for them.
 * Everything else is handled by LogbookExchangeFilterFunction
 */
class ConnectionErrorTracingFilterFunction(
   private val span: OperationTraceSpan,
) : ExchangeFilterFunction {
   override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
      return next.exchange(request)
         .doOnError { error ->
            val rootCause = Throwables.getRootCause(error)
            span.emitEvent(TracingEventKind.ERROR, SpanState.COMPLETE, null, ConnectionError(rootCause.message ?: "Failed to call ${request.url().toASCIIString()} - an unknown exception of type ${error::class.simpleName} occurred"), "Error")
         }
   }
}

class CapturingSink(
   private val span: OperationTraceSpan,
   private val requestBodyType: Type?,
) : Sink {
   private val responseTracingEvents = mutableListOf<TracingEvent>()
   var request: HttpRequest? = null
      private set;
   var response: HttpResponse? = null
      private set;

   /**
    * The most recent tracing event emitted for a response.
    * Use this to correlate payloads in data sources.
    */
   val lastResponseTracingEvent: TracingEvent?
      get() {
         return responseTracingEvents.lastOrNull()
      }

   override fun write(precorrelation: Precorrelation, request: HttpRequest) {
      val requestMetadata = com.orbitalhq.query.tracing.HttpRequest(
         request.requestUri,
         request.method,
         { request.bodyAsString },
         request.body.size.toLong(),
         request.headers.asVyneHeadersMap()
      )
      span.emitEvent(TracingEventKind.OK, SpanState.ACTIVE, requestBodyType, requestMetadata, request.method)
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
      val requestMetadata = com.orbitalhq.query.tracing.HttpResponse(
         response.status,
         { response.bodyAsString },
         response.body.size.toLong(),
         responseHeaders()
      )
      val eventKind = if (HttpStatus.valueOf(response.status).isError) {
         TracingEventKind.ERROR
      } else {
         TracingEventKind.OK
      }
      val responseEvent = span.emitEvent(eventKind, SpanState.COMPLETE, requestBodyType, requestMetadata, request.method)
      responseTracingEvents.add(responseEvent)
      this.request = request
      this.response = response
   }

}

fun HttpHeaders?.asVyneHeadersMap(): Map<String, List<String>> {
   return this?.toMutableMap()?.toMap() ?: emptyMap()
}
