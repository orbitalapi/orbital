package com.orbitalhq.cockpit.core.content

import com.google.common.base.Throwables
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Component
class ContentLoader(
   private val settings: ContentSettings,
   private val repositories: List<ContentRepository>
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val cache: Cache<ContentRequest, List<ContentCardData>> = CacheBuilder.newBuilder()
      .expireAfterWrite(settings.cacheDuration)
      .build<ContentRequest, List<ContentCardData>>()

   fun loadContent(request: ContentRequest): Mono<List<ContentCardData>> {
      return (Mono.justOrEmpty(cache.getIfPresent(request)) as Mono<List<ContentCardData>>)
         .switchIfEmpty {
            loadContentFromRepositories(request)
               .doOnNext { content ->
                  cache.put(request, content)
               }
               .switchIfEmpty { Mono.just(emptyList()) }
         }
   }

   private fun loadContentFromRepositories(request: ContentRequest): Mono<List<ContentCardData>> {
      return repositories.fold(Mono.empty()) { mono, repo ->
         mono.switchIfEmpty {
            try {
               repo.loadContent(request)
            } catch (e: Exception) {
               // HEre we're catching if the repo
               // threw before returning the Mono.
               // vs throwing inside the Mono,
               // which is naturally caught by onErrorResume below
               Mono.error(e)
            }.onErrorResume { e ->
                  val rootCause = Throwables.getRootCause(e)
                  logger.info { "Failed to load CMS data from ${repo::class.simpleName} - ${rootCause.message ?: rootCause::class.simpleName}" }
                  Mono.empty()
               }
         }
      }
   }
}

