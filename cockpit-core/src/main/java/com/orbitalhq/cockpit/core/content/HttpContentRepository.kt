package com.orbitalhq.cockpit.core.content

import com.google.common.base.Throwables
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
class HttpContentRepository(
   private val settings: ContentSettings,
   private val webClientBuilder: WebClient.Builder = WebClient.builder()
) : ContentRepository {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun loadContent(request: ContentRequest): Mono<List<ContentCardData>> {
      if (!settings.enabled || settings.url == null) {
         return Mono.empty()
      }
      return webClientBuilder.build()
         .post()
         .uri(settings.url)
         .bodyValue(request)
         .retrieve()
         .bodyToMono<List<ContentCardData>>()
         .doOnError { error ->
            val rootCause = Throwables.getRootCause(error)
            logger.warn { "Failed to load content from ${settings.url}: ${rootCause.message ?: rootCause::class.simpleName}" }
         }
         .onErrorResume {
            Mono.empty()
         }
   }
}
