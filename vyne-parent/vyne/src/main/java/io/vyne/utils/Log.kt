package io.vyne.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Any.log(): Logger {
   return LoggerFactory.getLogger(this::class.java)
}
