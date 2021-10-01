package io.vyne.utils

import java.io.InterruptedIOException

fun throwUnrecoverable(e: Exception) {
   when (e) {
      is InterruptedException,
      is InterruptedIOException,
      -> throw e
   }
}
