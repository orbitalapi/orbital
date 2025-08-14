package com.orbitalhq.query.runtime

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.orbitalhq.query.FailedQueryResponse
import com.orbitalhq.query.ProfilerOperation
import com.orbitalhq.schemas.Type
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

   @field:JsonIgnore
   override val responseType: Type? = null,
   @field:JsonIgnore
   override val responseHeaders: Map<String, List<String>>? = null,

   val exception: Exception? = null


) : FailedQueryResponse {
   override val queryResponseId: String = queryId
   @JsonProperty("responseType")
   @JsonInclude(JsonInclude.Include.NON_NULL)
   override val responseTypeName: String? = responseType?.paramaterizedName
}
