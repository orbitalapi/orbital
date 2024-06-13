package com.orbitalhq.cockpit.core.content

import reactor.core.publisher.Mono


data class ContentRequest(
   val version: String? = null,
   val location: String? = null
)

/**
 * A place we can fetch CMS content from to display in-app
 * Is generally either via an API, or default hard-coded values
 */
interface ContentRepository {
   fun loadContent(request:ContentRequest):Mono<List<ContentCardData>>
}



data class ContentCardData(
   val title: String,
   /**
    * Body text
    */
   val text: String,
   /**
    * If relative, should be relative with /assets as the base, and
    * be content that's already compiled into the codebase.
    * (Generally, use an icon from Tabler icons in /assets/img/tabler)
    * Remember that not all tabler icons are present.
    *
    * Alternatively, could be an absolute URL - but no caching is applied.
    *
    * Displayed 24x24
    */
   val iconUrl: String,

   val action: ContentCardAction
)

data class ContentCardAction(
   val text: String,
   /**
    * An angular route to navigate to within app (as an array of route
    * segments - eg ['query','editor'] becomes /query/editor.
    * Alternatively, provide a single value of an external url -
    * eg: https://foo.com
    */
   val route: List<String>
) {
   val external: Boolean
      get() {
         return route.size == 1 &&
            (route.single().startsWith("http://") || route.single().startsWith("https://"))
      }
}
