package io.vyne.cask.websocket

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.util.StringUtils
import org.springframework.web.util.UriUtils
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

private val QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?")

fun URI.queryParams(): MultiValueMap<String, String?>? {
   val queryParams: MultiValueMap<String, String?> = LinkedMultiValueMap()
   val query = this.rawQuery
   if (query != null) {
      val matcher = QUERY_PATTERN.matcher(query)
      while (matcher.find()) {
         val name = UriUtils.decode(matcher.group(1), StandardCharsets.UTF_8)
         val eq = matcher.group(2)
         var value = matcher.group(3)
         value = if (value != null) {
            UriUtils.decode(value, StandardCharsets.UTF_8)
         } else {
            if (StringUtils.hasLength(eq)) "" else null
         }
         queryParams.add(name, value)
      }
   }
   return queryParams
}
