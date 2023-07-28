package io.vyne.query.runtime.executor

import mu.KotlinLogging
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

/**
 * The spring LoadBalancerFilterFunctions are crazy complex,
 * and seem impossible to build without a dependency on a spring
 * context.
 *
 * This one's super simple.  Give it a discoveryClient.  If
 * the request is for a registered service, the url is rewritten.
 *
 */
class LoadBalancerFilterFunction(private val discoveryClient: DiscoveryClient) : ExchangeFilterFunction {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
      val host = request.url().host
      val instance = discoveryClient.getInstances(host)?.randomOrNull()
      return if (instance != null) {
         next.exchange(buildClientRequest(request, instance))
      } else {
         next.exchange(request)
      }
   }

   private fun buildClientRequest(request: ClientRequest, instance: ServiceInstance): ClientRequest {
      return ClientRequest
         .create(request.method(), LoadBalancerUriTools.reconstructURI(instance, request.url()))
         .headers { headers -> headers.addAll(request.headers()) }
         .cookies { cookies -> cookies.addAll(request.cookies()) }
         .attributes { attributes -> attributes.putAll(request.attributes()) }
         .body(request.body())
         .build()
   }
}
