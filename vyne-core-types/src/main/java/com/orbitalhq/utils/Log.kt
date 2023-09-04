package com.orbitalhq.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Any.log(): Logger {
   return LoggerFactory.getLogger(this::class.java)
}
fun Logger.debug(messageProvider: () -> String) {
   if (this.isDebugEnabled) {
      this.debug(messageProvider())
   }
}
