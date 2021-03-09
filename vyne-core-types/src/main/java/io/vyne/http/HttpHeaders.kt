package io.vyne.http

object HttpHeaders  {
   /**
    * Indicates that content has already preprocessed against the schema,
    * and that the returned results match the typed object schema, without requiring
    * further accessors.
    * Consumers should not evaluate accessors when parsing this content.
    */
   const val CONTENT_PREPARSED = "x-vyne-content-preparsed"
   const val STREAM_RECORD_COUNT = "x-vyne-content-record-count"

}
