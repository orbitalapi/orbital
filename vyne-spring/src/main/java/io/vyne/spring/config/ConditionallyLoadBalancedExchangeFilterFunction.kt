package io.vyne.spring.config

import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

/**
 * A filter function that allows preconfigured hosts to pass through
 * without attempting service discovery or load balancing.
 *
 * Useful for testing with hosts like localhost.
 */
class ConditionallyLoadBalancedExchangeFilterFunction(
   val hostsExcludedFromBalancing:List<String>,
   val delegate: ExchangeFilterFunction): ExchangeFilterFunction {
   override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
      return if (hostsExcludedFromBalancing.contains(request.url().host)) {
         next.exchange(request)
      } else {
         delegate.filter(request, next)
      }
   }
   companion object {
      fun permitLocalhost(delegate:ExchangeFilterFunction):ExchangeFilterFunction {
         return ConditionallyLoadBalancedExchangeFilterFunction(
            hostsExcludedFromBalancing = listOf("localhost"),
            delegate
         )
      }
   }
}
