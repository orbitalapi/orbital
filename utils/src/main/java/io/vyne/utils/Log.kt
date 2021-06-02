package io.vyne.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

// See https://projects.notional.uk/youtrack/articles/LENS-A-6/Logging-Conventions
// Place definition above class declaration, below imports,
// to make field static and accessible only within the file
//private val logger = KotlinLogging.logger {}
@Deprecated("Use private val logger = KotlinLogging.logger {} at the top of the file instead")
fun Any.log():Logger {
    return LoggerFactory.getLogger(this::class.java)
}
