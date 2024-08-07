package com.orbitalhq.query.runtime

import reactor.core.publisher.Flux
import java.security.Principal

/**
 * Component responsible for publishing the results of a streaming query.
 *
 * Streaming queries typically emit their results to a sink somewhere (such as a db,
 * or call a mutation).
 *
 * However, we often want to observe the results for a short-lived period of time.
 *
 * A StreamResultStreamProvider will connect to the output of the query, and
 * return it back to the inbound request (typically over either websocket or SSE)
 *
 */
interface StreamResultStreamProvider {
   fun getResultStream(streamName: String,principal: Principal?): Flux<Any>
}
