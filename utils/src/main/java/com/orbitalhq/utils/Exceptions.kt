package com.orbitalhq.utils

import java.io.InterruptedIOException

fun throwUnrecoverable(e: Exception) {
   when (e) {
      is InterruptedException,
      is InterruptedIOException,
      -> throw e
   }
}


/**
 * A performance optimized exception that doesn't provide stack information.
 * Use this when you want to throw an exception to signal a failed process, but
 * don't need to capture stack trace (ie., the exception is caught upstream, or
 * the failure is expected).
 *
 * Filling in the stack is expensive.
 */
abstract class NoStackException(message:String) : RuntimeException(message) {
   override fun fillInStackTrace(): Throwable {
      // Don't fill in the stack trace
      return this
   }
}
