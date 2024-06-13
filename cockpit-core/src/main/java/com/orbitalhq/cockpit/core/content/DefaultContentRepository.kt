package com.orbitalhq.cockpit.core.content

import reactor.core.publisher.Mono

/**
 * A content repository that returns hard-coded data.
 * This is the default / fallback for when loading from a remote service fails
 */
class DefaultContentRepository(
   private val content: List<ContentCardData>
) : ContentRepository {
   override fun loadContent(request: ContentRequest): Mono<List<ContentCardData>> {
      return Mono.just(content)
   }

}
