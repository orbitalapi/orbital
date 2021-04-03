package io.vyne.queryService

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class WebUiFilter : WebFilter {
   override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
      val path = exchange.request.uri.path
      // If the request is not for the /api, and does not contain a . (eg., main.js), then
      // redirect to index.  This means requrests to things like /query-wizard are rendereed by our Angular app
      return if (!path.startsWith("/api") && path.matches("[^\\\\.]*".toRegex())) {
         chain.filter(
            exchange
               .mutate().request(
                  exchange.request.mutate().path("/index.html").build()
               )
               .build()
         )
      } else {
         chain.filter(exchange)
      }
   }
}
