package com.orbitalhq.spring.invokers

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.orbitalhq.auth.schemes.MutualTls
import com.orbitalhq.schemas.Service
import com.orbitalhq.spring.http.auth.schemes.AuthWebClientCustomizer
import mu.KotlinLogging
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

private val logger = KotlinLogging.logger {  }

class WebClientFactory(private val webClientBuilder: WebClient.Builder,
                       private val authRequestCustomizer: AuthWebClientCustomizer) {
   // Map of WebClient per SSL Context.
   private val sslWebClientCache = CacheBuilder.newBuilder()
      .maximumSize(50)
      .removalListener<MutualTls, WebClient> {  removeNotification ->
         logger.info { "WebClient for mTls context ${removeNotification.key} removed from cache." }
      }.build(object: CacheLoader<MutualTls, WebClient>() {
         override fun load(mutualTls: MutualTls): WebClient {
           return  webClientBuilder.exchangeStrategies(exchangeStrategies)
               .clientConnector(authRequestCustomizer.reactorClientHttpConnector(mutualTls))
               .filter(authRequestCustomizer.authFromServiceNameAttribute)
               .build()
         }
      })

   private val exchangeStrategies =  ExchangeStrategies
      .builder()
      .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }.build()

   // WebClient for non-ssl requests.
   private val nonSslContextWebClient = webClientBuilder
      .exchangeStrategies(exchangeStrategies)
      .clientConnector(
         ReactorClientHttpConnector(
            authRequestCustomizer.httpClient(sslContext = null)
         )
      )
      .filter(authRequestCustomizer.authFromServiceNameAttribute)
      .build()

   fun webClientFor(service: Service): WebClient {
      val sslWebClient =  authRequestCustomizer
         .mutualMtlsAuthScheme(service.fullyQualifiedName)?.let { mutualTls ->
          sslWebClientCache.get(mutualTls)
      }
      return  sslWebClient ?: nonSslContextWebClient
   }
}
