package org.taxilang.playground

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono


/**
 * Handles requests intended for our web app (ie., everything not at /api)
 * and forwards them down to index.html, to allow angular to handle the
 * routing
 */
@Component
class Html5UrlSupportFilter(
   @Value("\${management.endpoints.web.base-path:/actuator}") private val actuatorPath: String
) : WebFilter {
   companion object {
      val ASSET_EXTENSIONS =
         listOf(".css", ".js", ".js?", ".js.map", ".html", ".scss", ".ts", ".ttf", ".wott", ".svg", ".gif", ".png")
   }

   override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
      val path = exchange.request.uri.path
      // If the request is not for the /api, and does not contain a . (eg., main.js), then
      // redirect to index.  This means requrests to things like /query-wizard are rendereed by our Angular app
      return when {
         path.startsWith("/api") -> {
            chain.filter(exchange)
         }

         path.startsWith(actuatorPath) -> {
            chain.filter(exchange)
         }

         ASSET_EXTENSIONS.any { path.endsWith(it) } -> chain.filter(exchange)
         else -> {
            // These are requests that aren't /api, and don't have an asset extension (like .js), so route it to the
            // angular app
            chain.filter(
               exchange
                  .mutate().request(
                     exchange.request.mutate().path("/index.html").build()
                  )
                  .build()
            )
         }
      }
   }
}
