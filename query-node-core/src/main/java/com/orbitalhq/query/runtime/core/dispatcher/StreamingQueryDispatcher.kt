package com.orbitalhq.query.runtime.core.dispatcher

import com.orbitalhq.query.ResultMode
import org.springframework.http.MediaType
import reactor.core.publisher.Flux

interface StreamingQueryDispatcher {
   fun dispatchQuery(
      query: String,
      clientQueryId: String,
      mediaType: String = MediaType.APPLICATION_JSON_VALUE,
      resultMode: ResultMode = ResultMode.RAW,
      arguments: Map<String, Any?> = emptyMap()
   ): Flux<Any>
}
