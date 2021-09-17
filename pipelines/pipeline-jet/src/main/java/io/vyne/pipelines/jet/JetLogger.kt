package io.vyne.pipelines.jet

import com.hazelcast.logging.ILogger
import com.hazelcast.logging.Logger
import io.vyne.pipelines.jet.api.transport.PipelineLogger
import kotlin.reflect.KClass

typealias JetPipelineLogger = ILogger

/**
 * Wrapper object that sits in front of Hazelcast Jet ILogger.
 * Does nothing but try to make our code clearer about whwere we're logging to the Jet
 * logging infrastructure.
 */
object JetLogger {
   private val pipelineLoggerDecorators = mutableMapOf<KClass<*>, VynePipelineLoggerJetFacade>()
   fun getLogger(clazz: KClass<*>): JetPipelineLogger {
      return Logger.getLogger(clazz.java)
   }

   fun getLogger(clazz: Class<*>): JetPipelineLogger {
      return Logger.getLogger(clazz)
   }

   /**
    * Returns a Jet ILogger facade that implements the old Vyne PipelineLogger
    * interface.
    */
   fun getVynePipelineLogger(clazz: KClass<*>): PipelineLogger {
      return pipelineLoggerDecorators.getOrPut(clazz) {
         VynePipelineLoggerJetFacade(getLogger(clazz))
      }
   }
}

class VynePipelineLoggerJetFacade(private val logger: ILogger) : PipelineLogger {
   override fun debug(message: () -> String) {
      logger.fine(message())
   }

   override fun info(message: () -> String) {
      logger.info(message())
   }

   override fun warn(message: () -> String) {
      logger.warning(message())
   }

   override fun error(message: () -> String) {
      logger.severe(message())
   }

   override fun error(exception: Throwable, message: () -> String) {
      logger.severe(message(), exception)
   }

}

