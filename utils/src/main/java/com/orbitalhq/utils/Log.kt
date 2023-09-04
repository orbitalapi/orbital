package com.orbitalhq.utils

import mu.KLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// See https://projects.notional.uk/youtrack/articles/LENS-A-6/Logging-Conventions
// Place definition above class declaration, below imports,
// to make field static and accessible only within the file
//private val logger = KotlinLogging.logger {}
@Deprecated("Use private val logger = KotlinLogging.logger {} at the top of the file instead")
fun Any.log(): Logger {
   return LoggerFactory.getLogger(this::class.java)
}

fun KLogger.withQueryId(queryId: String): DecoratedLogger {
   return DecoratedLogger(this, queryId)
}

class DecoratedLogger(private val underlyingLogger: KLogger, private val prefix: String) {

   fun trace(msg: () -> Any?) = underlyingLogger.trace(decorate(msg))

   /** Lazy add a log message if isDebugEnabled is true */
   fun debug(msg: () -> Any?) = underlyingLogger.debug(decorate(msg))

   /** Lazy add a log message if isInfoEnabled is true */
   fun info(msg: () -> Any?) = underlyingLogger.info(decorate(msg))

   /** Lazy add a log message if isWarnEnabled is true */
   fun warn(msg: () -> Any?) = underlyingLogger.warn(decorate(msg))

   /** Lazy add a log message if isErrorEnabled is true */
   fun error(msg: () -> Any?) = underlyingLogger.error(decorate(msg))

   /** Lazy add a log message with throwable payload if isTraceEnabled is true */
   fun trace(t: Throwable?, msg: () -> Any?) = underlyingLogger.trace(t, decorate(msg))

   /** Lazy add a log message with throwable payload if isDebugEnabled is true */
   fun debug(t: Throwable?, msg: () -> Any?) = underlyingLogger.debug(t, decorate(msg))

   /** Lazy add a log message with throwable payload if isInfoEnabled is true */
   fun info(t: Throwable?, msg: () -> Any?) = underlyingLogger.info(t, decorate(msg))

   /** Lazy add a log message with throwable payload if isWarnEnabled is true */
   fun warn(t: Throwable?, msg: () -> Any?) = underlyingLogger.warn(t, decorate(msg))

   /** Lazy add a log message with throwable payload if isErrorEnabled is true */
   fun error(t: Throwable?, msg: () -> Any?) = underlyingLogger.error(t, decorate(msg))

   private fun decorate(callback: () -> Any?): () -> Any? {
      return {
         "[$prefix] ${callback()}"
      }
   }
}
