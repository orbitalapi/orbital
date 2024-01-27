package com.orbitalhq.spring.invokers

import kotlinx.datetime.Clock
import org.springframework.http.HttpHeaders
import kotlin.time.Duration.Companion.seconds

data class HttpCacheControl(
   val noCache: Boolean,
   /** If true, this response should not be cached. */
   val noStore: Boolean,
   /** The duration past the response's served date that it can be served without validation. */
   val maxAgeSeconds: Int,
   /**
    * The "s-maxage" directive is the max age for shared caches. Not to be confused with "max-age"
    * for non-shared caches, As in Firefox and Chrome, this directive is not honored by this cache.
    */
   val sMaxAgeSeconds: Int,
   val isPrivate: Boolean,
   val isPublic: Boolean,
   val mustRevalidate: Boolean,
   val maxStaleSeconds: Int,
   val minFreshSeconds: Int,
   /**
    * This field's name "only-if-cached" is misleading. It actually means "do not use the network".
    * It is set by a client who only wants to make a request if it can be fully satisfied by the
    * cache. Cached responses that would require validation (ie. conditional gets) are not permitted
    * if this header is set.
    */
   val onlyIfCached: Boolean,
   val noTransform: Boolean,
   val immutable: Boolean
) {

   fun expiredAt() = if (maxAgeSeconds != -1 && !noCache) Clock.System.now().plus(maxAgeSeconds.seconds) else null
   companion object {
      fun parse(httpHeaders: HttpHeaders): HttpCacheControl? {
         var noCache = false
         var noStore = false
         var maxAgeSeconds = -1
         var sMaxAgeSeconds = -1
         var isPrivate = false
         var isPublic = false
         var mustRevalidate = false
         var maxStaleSeconds = -1
         var minFreshSeconds = -1
         var onlyIfCached = false
         var noTransform = false
         var immutable = false



         return httpHeaders.cacheControl?.let { cacheControlValue ->
           val trimmedValues =  cacheControlValue.split(',').map { it.trim() }
            trimmedValues.forEach { value ->
               val nameValue = value.split('=')
               val directive = nameValue.first().trim()
               val parameter = if (nameValue.size == 2) nameValue[1] else null

               when {
                  "no-cache".equals(directive, ignoreCase = true) -> {
                     noCache = true
                  }

                  "no-store".equals(directive, ignoreCase = true) -> {
                     noStore = true
                  }

                  "max-age".equals(directive, ignoreCase = true) -> {
                     maxAgeSeconds = parameter.toNonNegativeInt(-1)
                  }

                  "s-maxage".equals(directive, ignoreCase = true) -> {
                     sMaxAgeSeconds = parameter.toNonNegativeInt(-1)
                  }

                  "private".equals(directive, ignoreCase = true) -> {
                     isPrivate = true
                  }

                  "public".equals(directive, ignoreCase = true) -> {
                     isPublic = true
                  }

                  "must-revalidate".equals(directive, ignoreCase = true) -> {
                     mustRevalidate = true
                  }

                  "max-stale".equals(directive, ignoreCase = true) -> {
                     maxStaleSeconds = parameter.toNonNegativeInt(Int.MAX_VALUE)
                  }

                  "min-fresh".equals(directive, ignoreCase = true) -> {
                     minFreshSeconds = parameter.toNonNegativeInt(-1)
                  }

                  "only-if-cached".equals(directive, ignoreCase = true) -> {
                     onlyIfCached = true
                  }

                  "no-transform".equals(directive, ignoreCase = true) -> {
                     noTransform = true
                  }

                  "immutable".equals(directive, ignoreCase = true) -> {
                     immutable = true
                  }
               }
            }

             HttpCacheControl(
               noCache = noCache,
               noStore = noStore,
               maxAgeSeconds = maxAgeSeconds,
               sMaxAgeSeconds = sMaxAgeSeconds,
               isPrivate = isPrivate,
               isPublic = isPublic,
               mustRevalidate = mustRevalidate,
               maxStaleSeconds = maxStaleSeconds,
               minFreshSeconds = minFreshSeconds,
               onlyIfCached = onlyIfCached,
               noTransform = noTransform,
               immutable = immutable
            )
         }
      }
   }
}
internal fun String?.toNonNegativeInt(defaultValue: Int): Int {
   try {
      val value = this?.toLong() ?: return defaultValue
      return when {
         value > Int.MAX_VALUE -> Int.MAX_VALUE
         value < 0 -> 0
         else -> value.toInt()
      }
   } catch (_: NumberFormatException) {
      return defaultValue
   }
}
