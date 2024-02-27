package com.orbitalhq.query.runtime.core.dispatcher

import com.orbitalhq.query.ResultMode
import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface StreamingQueryDispatcher {
   /**
    * Dispatches the query to the remote executor, and returns the results.
    * Remote execute queries do not currently support streaming, so results are Mono<Any>.
    *
    * Note : At the time of implementation, we really only support a single remote executor, which is
    * an AWS Lambda, executed via HTTP(s).  At the time, lambda's didn't support writing streaming responses
    * (this has now changed).  We could upgrade to support streaming results.
    *
    * However, if/when we do, the response type here would become Flux<Any>. The remote service would need
    * to provide some mechanism to indicate if the returned result is an array or an object.
    * Otherwise, we end up with all results serialized as an array, which I've literally just fixed.
    *
    */
   fun dispatchQuery(
      query: String,
      clientQueryId: String,
      mediaType: String = MediaType.APPLICATION_JSON_VALUE,
      resultMode: ResultMode = ResultMode.RAW,
      arguments: Map<String, Any?> = emptyMap()
   ): Mono<Any>
}
