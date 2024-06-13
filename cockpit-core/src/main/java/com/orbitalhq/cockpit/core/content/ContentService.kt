package com.orbitalhq.cockpit.core.content

import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


/**
 * API for returning content shown in-app
 */
@RestController
class ContentService(
   private val loader: ContentLoader,
   private val buildInfo: BuildProperties? = null
) {

   @GetMapping("/api/cms/content")
   fun getContent(): Mono<List<ContentCardData>> {
      val request = ContentRequest(
         version = buildInfo?.version,
         location = null
      )
      return loader.loadContent(request)
   }

}

