package io.vyne.query.runtime

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.query.FailedQueryResponse
import io.vyne.query.ProfilerOperation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class FailedSearchResponse(
   override val message: String,
   @field:JsonIgnore // this sends too much information - need to build a lightweight version
   override val profilerOperation: ProfilerOperation?,
   override val queryId: String,
   val results: Map<String, Any?> = mapOf(),
   override val clientQueryId: String? = null,
   override val responseType: String? = null


) : FailedQueryResponse {
   override val queryResponseId: String = queryId
}
