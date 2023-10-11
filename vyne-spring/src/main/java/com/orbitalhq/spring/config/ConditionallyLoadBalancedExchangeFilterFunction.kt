package com.orbitalhq.spring.config

import mu.KotlinLogging
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
   val hostsExcludedFromBalancing: List<String>,
   /**
    * If set, only the services in the whitelist are loadbalanced,
    * everything else is deferred to the delegate
    */
   val loadBalanceWhitelist: List<String>? = null,
   val delegate: ExchangeFilterFunction
) : ExchangeFilterFunction {
   override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
      val host = request.url().host
      return if (hostsExcludedFromBalancing.contains(host)) {
         next.exchange(request)
      } else if (loadBalanceWhitelist != null) {
         if (loadBalanceWhitelist.contains(host)) {
            // Load balance this through the delegate, as it's a known host
            delegate.filter(request, next)
         } else {
            next.exchange(request)
         }
      } else {
         delegate.filter(request, next)
      }
   }

   companion object {
      private val logger = KotlinLogging.logger {}

      /**
       * Load balancer which only attempts to load balance pre-configured
       * hosts.
       * Everything else is left untouched.
       * This allows load balancing at the network level to take over.
       * Also, doesn't attempt to rewrite unknown hosts (like localhost),
       * which with a default config become unusable
       */
      fun onlyKnownHosts(hosts: List<String>, delegate: ExchangeFilterFunction): ExchangeFilterFunction {
         logger.info {
            "Conditional client-side load balancer is active.  Requests to the following hosts are rewritten: ${
               hosts.joinToString(
                  ", "
               )
            }"
         }
         return ConditionallyLoadBalancedExchangeFilterFunction(
            emptyList(),
            hosts,
            delegate
         )
      }

      fun permitLocalhost(delegate: ExchangeFilterFunction): ExchangeFilterFunction {
         return ConditionallyLoadBalancedExchangeFilterFunction(
            hostsExcludedFromBalancing = listOf("localhost"),
            null,
            delegate
         )
      }
   }
}
