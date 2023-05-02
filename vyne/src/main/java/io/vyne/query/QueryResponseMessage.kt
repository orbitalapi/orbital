package io.vyne.query

/**
 * Spiritual successor to QueryResponse and it's subclasses.
 * Simplifies the API design, given lessons learnt moving to streaming queries.
 * Supports signalling a complete response (for request-reply), a streaming response (for streaming queries),
 * a end of stream message, or an error.
 */
data class QueryResponseMessage(
   /**
    * Null when receiving an END_OF_STREAM or ERROR
    */
   val payload: Any?,
   val messageKind: QueryResponseMessageKind,
   /**
    * The error message, if the query failed.
    */
   val message: String? = null
) {
   companion object {
      val COMPLETED = QueryResponseMessage(null, QueryResponseMessageKind.END_OF_STREAM)
      fun error(message:String) = QueryResponseMessage(null, QueryResponseMessageKind.ERROR, message)
      fun singleResult(payload:Any?) = QueryResponseMessage(payload, QueryResponseMessageKind.RESULT)
      fun streamResult(payload: Any?) = QueryResponseMessage(payload, QueryResponseMessageKind.STREAM_MESSAGE)
   }
   enum class QueryResponseMessageKind(val isFinalMessage: Boolean) {
      /**
       * A message within the stream.  There may be more messages to come.
       */
      STREAM_MESSAGE(false),

      /**
       * The stream has completed, there are no more results
       */
      END_OF_STREAM(true),

      /**
       * The entire result.
       */
      RESULT(true),

      /**
       * The query has failed.
       */
      ERROR(true)


   }
}
