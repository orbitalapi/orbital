package com.orbitalhq.query.runtime

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.orbitalhq.query.FailedQueryResponse
import com.orbitalhq.query.ProfilerOperation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class FailedSearchResponse(
   override val message: String,
   @field:JsonIgnore // this sends too much information - need to build a lightweight version
   override val profilerOperation: ProfilerOperation? = null,
   @JsonInclude(JsonInclude.Include.NON_NULL)
   override val queryId: String,
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   val results: Map<String, Any?> = mapOf(),
   @JsonInclude(JsonInclude.Include.NON_NULL)
   override val clientQueryId: String? = null,
   @JsonInclude(JsonInclude.Include.NON_NULL)
   override val responseType: String? = null


) : FailedQueryResponse {
   override val queryResponseId: String = queryId
}
