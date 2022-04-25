package io.vyne.schema.http

enum class HttpKeepAliveStrategy {

   // If changing these values, also update their string references in config classes.
   // Search for string references.
   None,
   HttpPoll
}
