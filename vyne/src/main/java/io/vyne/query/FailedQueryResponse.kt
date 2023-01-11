package io.vyne.query

interface FailedQueryResponse : QueryResponse {
   val message: String
   override val responseStatus: QueryResponse.ResponseStatus
      get() = QueryResponse.ResponseStatus.ERROR
   override val isFullyResolved: Boolean
      get() = false
}
