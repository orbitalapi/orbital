package io.vyne.pipelines.jet

import com.hazelcast.logging.ILogger
import com.hazelcast.logging.Logger
import kotlin.reflect.KClass

typealias JetPipelineLogger = ILogger

/**
 * Wrapper object that sits in front of Hazelcast Jet ILogger.
 * Does nothing but try to make our code clearer about where we're logging to the Jet
 * logging infrastructure.
 */
object JetLogger {
   fun getLogger(clazz: KClass<*>): JetPipelineLogger {
      return Logger.getLogger(clazz.java)
   }
}
